package com.petcare.backend.service;

import com.petcare.backend.config.ActivityColdDataRabbitMQConfig;
import com.petcare.backend.dto.ColdStorageEvent;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 冷数据迁移事件消费者服务（简化版 + 幂等性保证）
 * 
 * 简化设计：
 * 1. 迁移成功后直接删除 MySQL 记录
 * 2. 幂等性：消费前检查 HBase 是否已存在数据
 * 3. MQ 负责重试，不需要 MySQL 记录重试次数
 * 
 * 幂等性保证：
 * 1. 写入 HBase 前检查是否已存在（通过 RowKey 查询）
 * 2. 如果 HBase 已存在，直接删除 MySQL 记录并返回成功
 * 3. HBase 的 saveToColdStorage 方法内部也会检查是否已存在
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ColdStorageEventConsumer {

    private final ActivityRecordRepository activityRecordRepository;
    private final HBaseColdStorageService hBaseColdStorageService;

    // 解冻临时访问窗口（分钟）
    private static final int THAW_ACCESS_MINUTES = 10;

    /**
     * 处理冷存储事件
     */
    @RabbitListener(queues = ActivityColdDataRabbitMQConfig.COLD_MIGRATION_QUEUE)
    @Transactional
    public void handleColdStorageEvent(ColdStorageEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ消费】收到冷存储事件: eventId={}, operationType={}, activityRecordId={}",
                event.getEventId(), event.getOperationType(), event.getActivityRecordId());

        try {
            switch (event.getOperationType()) {
                case MIGRATE_TO_COLD -> handleMigrateToCold(event);
                case THAW_FROM_COLD -> handleThawFromCold(event);
                case DELETE_FROM_COLD -> handleDeleteFromCold(event);
                default -> log.warn("【MQ消费】未知的操作类型: eventId={}, operationType={}",
                        event.getEventId(), event.getOperationType());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ消费】冷存储事件处理完成: eventId={}, operationType={}, activityRecordId={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), event.getActivityRecordId(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ消费】冷存储事件处理失败: eventId={}, operationType={}, activityRecordId={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), event.getActivityRecordId(), elapsed, e.getMessage(), e);
            // 抛出异常触发 MQ 重试（由 RabbitMQ 配置决定重试次数）
            throw e;
        }
    }

    /**
     * 处理迁移到冷库（保证幂等性）
     * 
     * 幂等性保证流程：
     * 1. 查询 MySQL 记录（可能已被之前的消息删除）
     * 2. 如果记录不存在，说明已被迁移，直接返回成功
     * 3. 生成 RowKey 并检查 HBase 是否已存在
     * 4. 如果 HBase 已存在，直接删除 MySQL 记录
     * 5. 如果 HBase 不存在，写入 HBase 后删除 MySQL 记录
     */
    private void handleMigrateToCold(ColdStorageEvent event) {
        log.info("【MQ消费】开始迁移到冷库: eventId={}, activityRecordId={}",
                event.getEventId(), event.getActivityRecordId());

        // 1. 从MySQL查询记录
        ActivityRecord record = activityRecordRepository.findById(event.getActivityRecordId()).orElse(null);
        if (record == null) {
            // 记录不存在，可能已被之前的消息迁移并删除
            log.info("【MQ消费】活动记录不存在（可能被其他消息已迁移删除）: activityRecordId={}", 
                    event.getActivityRecordId());
            return;
        }

        // 2. 检查状态是否正确（必须是 MIGRATING）
        if (!"MIGRATING".equals(record.getMigrationStatus())) {
            log.warn("【MQ消费】活动记录状态不正确: activityRecordId={}, status={}", 
                    event.getActivityRecordId(), record.getMigrationStatus());
            // 状态不对，可能是重试，直接返回（不重复处理）
            return;
        }

        // 3. 生成 RowKey
        String rowKey = hBaseColdStorageService.generateRowKey(
                record.getPet() != null ? record.getPet().getPetId() : 0L,
                record.getActivityDate(),
                record.getActivityRecordId()
        );

        // 4. 【幂等性检查】检查 HBase 是否已存在
        if (hBaseColdStorageService.existsInColdStorage(rowKey)) {
            log.info("【MQ消费】HBase中已存在数据（幂等跳过），直接删除MySQL记录: activityRecordId={}, rowKey={}", 
                    event.getActivityRecordId(), rowKey);
            // HBase 已存在，直接删除 MySQL 记录
            activityRecordRepository.delete(record);
            return;
        }

        // 5. 转换为DTO并写入HBase
        ActivityRecordDTO dto = convertToDTO(record);
        hBaseColdStorageService.saveToColdStorage(dto);

        // 6. 删除 MySQL 记录（迁移成功后直接删除）
        activityRecordRepository.delete(record);

        log.info("【MQ消费】迁移到冷库完成，MySQL记录已删除: activityRecordId={}, rowKey={}", 
                event.getActivityRecordId(), rowKey);
    }

    /**
     * 处理从冷库解冻
     * 
     * 解冻策略：
     * 1. 如果仍在10分钟有效期内，直接更新时间戳并返回
     * 2. 如果已过期，发送解冻请求（将存储类型改回STANDARD）
     * 3. 设置新的10分钟临时访问过期时间
     * 
     * 注意：由于简化后 MySQL 记录迁移后会被删除，
     * 解冻逻辑需要特殊处理：查询 HBase 中的数据并返回
     */
    private void handleThawFromCold(ColdStorageEvent event) {
        log.info("【MQ消费】处理解冻请求: eventId={}, activityRecordId={}",
                event.getEventId(), event.getActivityRecordId());

        // 1. 从 MySQL 查询记录
        ActivityRecord record = activityRecordRepository.findById(event.getActivityRecordId()).orElse(null);
        if (record == null) {
            log.warn("【MQ消费】活动记录不存在，无法解冻: activityRecordId={}", event.getActivityRecordId());
            return;
        }

        // 2. 检查解冻是否过期（10分钟临时访问）
        LocalDateTime now = LocalDateTime.now();
        if (record.getThawExpireTime() != null && now.isBefore(record.getThawExpireTime())) {
            // 仍在有效期内，更新过期时间并返回
            log.info("【MQ消费】解冻仍在有效期内，刷新过期时间: activityRecordId={}", event.getActivityRecordId());
            record.setThawExpireTime(now.plusMinutes(THAW_ACCESS_MINUTES));
            activityRecordRepository.save(record);
            return;
        }

        // 3. 生成 RowKey 并检查 HBase 是否存在
        String rowKey = hBaseColdStorageService.generateRowKey(
                record.getPet() != null ? record.getPet().getPetId() : 0L,
                record.getActivityDate(),
                record.getActivityRecordId()
        );

        if (!hBaseColdStorageService.existsInColdStorage(rowKey)) {
            log.warn("【MQ消费】HBase中数据不存在: activityRecordId={}, rowKey={}", 
                    event.getActivityRecordId(), rowKey);
            return;
        }

        // 4. 设置新的10分钟过期时间
        record.setThawExpireTime(now.plusMinutes(THAW_ACCESS_MINUTES));
        activityRecordRepository.save(record);

        log.info("【MQ消费】解冻处理完成，临时访问有效期至: activityRecordId={}, expireTime={}", 
                event.getActivityRecordId(), record.getThawExpireTime());
    }

    /**
     * 处理从冷库删除
     * 
     * 注意：由于简化后 MySQL 记录迁移后会被删除，
     * 删除操作只删除 HBase 中的数据，不涉及 MySQL
     */
    private void handleDeleteFromCold(ColdStorageEvent event) {
        log.info("【MQ消费】处理删除冷库数据: eventId={}, activityRecordId={}",
                event.getEventId(), event.getActivityRecordId());

        // 生成 RowKey
        String rowKey = hBaseColdStorageService.generateRowKey(
                event.getPetId() != null ? event.getPetId() : 0L,
                event.getActivityDate(),
                event.getActivityRecordId()
        );

        // 从HBase删除
        if (hBaseColdStorageService.existsInColdStorage(rowKey)) {
            hBaseColdStorageService.deleteFromColdStorage(rowKey);
            log.info("【MQ消费】删除冷库数据完成: activityRecordId={}, rowKey={}", 
                    event.getActivityRecordId(), rowKey);
        } else {
            log.info("【MQ消费】HBase中数据不存在，无需删除: activityRecordId={}, rowKey={}", 
                    event.getActivityRecordId(), rowKey);
        }
    }

    /**
     * 转换为DTO
     */
    private ActivityRecordDTO convertToDTO(ActivityRecord record) {
        ActivityRecordDTO dto = new ActivityRecordDTO();
        dto.setActivityRecordId(record.getActivityRecordId());
        dto.setActivityDate(record.getActivityDate());
        dto.setActivityDescription(record.getActivityDescription());
        
        if (record.getActivity() != null) {
            dto.setActivityId(record.getActivity().getActivityId());
            dto.setActivityName(record.getActivity().getActivityName());
            if (record.getActivity().getActivityKind() != null) {
                dto.setActivityKindId(record.getActivity().getActivityKind().getActivityKindId());
                dto.setActivityKindName(record.getActivity().getActivityKind().getActivityKindName());
            }
        }
        
        if (record.getPet() != null) {
            dto.setPetId(record.getPet().getPetId());
            dto.setPetName(record.getPet().getName());
            if (record.getPet().getUser() != null) {
                dto.setUserId(record.getPet().getUser().getUserId());
            }
        }
        
        return dto;
    }
}
