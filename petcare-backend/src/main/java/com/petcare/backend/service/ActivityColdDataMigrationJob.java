package com.petcare.backend.service;

import com.petcare.backend.dto.ColdStorageEvent;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.repository.ActivityRecordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ActivityRecord 冷数据迁移定时任务（简化版）
 * 
 * 简化设计：
 * 1. 只保留 NONE/MIGRATING 两种状态
 * 2. 迁移成功后直接删除 MySQL 记录
 * 3. MQ 负责重试，不需要 MySQL 记录重试次数
 * 
 * 执行流程：
 * 1. 扫描 MySQL 中超过30天的记录
 * 2. 直接发布迁移任务到 MQ（不再分打标/发布两阶段）
 * 3. MQ 消费者执行实际迁移（HBase写入 + MySQL删除）
 * 
 * 幂等性保证：
 * 1. 对于 NONE 状态：发布 MQ 消息
 * 2. 对于 MIGRATING 状态（可能是之前卡住的）：
 *    - 直接检查 HBase 是否存在
 *    - 存在则删除 MySQL 记录
 *    - 不存在则直接写入 HBase 后删除 MySQL 记录
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityColdDataMigrationJob {

    private final ActivityRecordRepository activityRecordRepository;
    private final ColdStorageEventPublisher coldStorageEventPublisher;
    private final HBaseColdStorageService hBaseColdStorageService;
    private final TransactionTemplate transactionTemplate;

    @Value("${hbase.cold-data.activity-record.days-threshold:30}")
    private int daysThreshold;

    @Value("${hbase.cold-data.activity-record.batch-size:1000}")
    private int batchSize;

    @Value("${hbase.cold-data.activity-record.migration-cron:0 0 2 * * ?}")
    private String migrationCron;

    @PostConstruct
    public void init() {
        log.info("【定时任务初始化】ActivityRecord冷数据迁移任务已注册");
        log.info("【定时任务初始化】cron表达式: {}", migrationCron);
        log.info("【定时任务初始化】days-threshold: {}, batch-size: {}", daysThreshold, batchSize);
    }

    /**
     * 每天北京时间 2:00 执行冷数据迁移任务
     * 
     * cron表达式说明：
     * - 秒(0) 分(0) 时(2) 日(*) 月(*) 周(?)
     * - 北京时间2:00 = UTC 18:00
     */
    @Scheduled(cron = "${hbase.cold-data.activity-record.migration-cron:0 0 2 * * ?}", zone = "Asia/Shanghai")
    @Transactional
    public void migrateToColdStorage() {
        log.info("================= ActivityRecord 冷数据迁移任务开始 =================");

        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);

        // 1. 处理新发现的待迁移记录（状态为 NONE）
        int newPublished = scanAndPublishMigrationTasks(threshold);

        // 2. 处理之前卡住的记录（状态为 MIGRATING，可能是 MQ 消费失败）
        int stuckResolved = resolveStuckMigrations(threshold);

        log.info("================= ActivityRecord 冷数据迁移任务完成 =================");
        log.info("新增发布迁移任务数: {}, 处理卡住记录数: {}", newPublished, stuckResolved);
    }

    /**
     * 扫描并发布迁移任务（状态为 NONE 的记录）
     *
     * 采用乐观锁机制：
     * 1. 先尝试原子性更新状态为 MIGRATING
     * 2. 更新成功才发布 MQ 消息
     * 3. 如果 MQ 发布失败，需要回滚状态
     */
    private int scanAndPublishMigrationTasks(LocalDateTime threshold) {
        log.info("【新迁移】开始扫描待迁移记录，超过 {} 天", daysThreshold);

        int publishedCount = 0;
        int pageNumber = 0;

        while (true) {
            // 分页查询待迁移的记录（状态为 NONE）
            Page<ActivityRecord> page = activityRecordRepository
                    .findRecordsToMigrate(threshold, PageRequest.of(pageNumber, batchSize));

            if (page.isEmpty()) {
                break;
            }

            for (ActivityRecord record : page.getContent()) {
                Long recordId = record.getActivityRecordId();

                // 1. 尝试原子性获取锁（乐观锁）
                int updated = activityRecordRepository.updateMigrationStatus(recordId, "NONE", "MIGRATING");
                if (updated == 0) {
                    // 状态不是 NONE，可能被其他线程或上次任务处理，跳过
                    log.debug("【新迁移】记录状态不是 NONE，跳过: activityRecordId={}", recordId);
                    continue;
                }

                try {
                    // 2. 发布迁移消息到 MQ
                    coldStorageEventPublisher.publishMigrateToColdEvent(
                            recordId,
                            record.getPet() != null ? record.getPet().getPetId() : null,
                            record.getActivity() != null ? record.getActivity().getActivityId() : null,
                            record.getActivityDate()
                    );
                    publishedCount++;
                    log.debug("【新迁移】已发布迁移任务: activityRecordId={}", recordId);

                } catch (Exception e) {
                    log.error("【新迁移】发送迁移任务失败，回滚状态: activityRecordId={}, error={}",
                            recordId, e.getMessage());
                    // 回滚状态
                    try {
                        activityRecordRepository.updateMigrationStatus(recordId, "MIGRATING", "NONE");
                    } catch (Exception rollbackEx) {
                        log.error("【新迁移】回滚状态失败: activityRecordId={}, error={}",
                                recordId, rollbackEx.getMessage());
                    }
                }
            }

            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        log.info("【新迁移】完成，共发布 {} 条迁移任务", publishedCount);
        return publishedCount;
    }

    /**
     * 处理卡住的迁移记录（状态为 MIGRATING）
     * 
     * 这些记录可能是之前扫描时发布 MQ 消息后，MQ 消费失败导致的。
     * 处理逻辑：
     * 1. 查询所有状态为 MIGRATING 的记录
     * 2. 检查 HBase 是否已存在该记录
     * 3. 如果存在：直接删除 MySQL 记录（幂等）
     * 4. 如果不存在：直接写入 HBase，然后删除 MySQL 记录
     * 
     * 注意：这里不使用 MQ，因为：
     * - 如果 HBase 已有数据，说明消息已经消费成功了，直接清理 MySQL 即可
     * - 如果 HBase 没有数据，直接重试写入即可
     */
    private int resolveStuckMigrations(LocalDateTime threshold) {
        log.info("【卡住处理】开始处理卡住的迁移记录");

        // 查询所有状态为 MIGRATING 的记录
        List<ActivityRecord> stuckRecords = activityRecordRepository.findMigratingRecords(PageRequest.of(0, batchSize));

        int resolvedCount = 0;

        for (ActivityRecord record : stuckRecords) {
            try {
                // 生成 RowKey
                String rowKey = hBaseColdStorageService.generateRowKey(
                        record.getPet() != null ? record.getPet().getPetId() : 0L,
                        record.getActivityDate(),
                        record.getActivityRecordId()
                );

                // 检查 HBase 是否已存在
                if (hBaseColdStorageService.existsInColdStorage(rowKey)) {
                    // HBase 已有数据，说明迁移已完成，直接删除 MySQL 记录
                    log.info("【卡住处理】HBase已存在，直接删除MySQL记录: activityRecordId={}, rowKey={}",
                            record.getActivityRecordId(), rowKey);
                    activityRecordRepository.delete(record);
                } else {
                    // HBase 没有数据，需要重新写入
                    log.info("【卡住处理】HBase不存在，重新写入: activityRecordId={}, rowKey={}",
                            record.getActivityRecordId(), rowKey);
                    
                    // 转换为 DTO
                    ActivityRecordDTO dto = convertToDTO(record);
                    
                    // 写入 HBase
                    hBaseColdStorageService.saveToColdStorage(dto);
                    
                    // 删除 MySQL 记录
                    activityRecordRepository.delete(record);
                }
                resolvedCount++;

            } catch (Exception e) {
                log.error("【卡住处理】处理卡住记录失败: activityRecordId={}, error={}",
                        record.getActivityRecordId(), e.getMessage());
            }
        }

        log.info("【卡住处理】完成，共处理 {} 条卡住记录", resolvedCount);
        return resolvedCount;
    }

    /**
     * 转换为 DTO
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

    /**
     * 手动触发迁移任务（用于测试或紧急迁移）
     */
    public void manualMigrate() {
        log.info("手动触发冷数据迁移任务");
        transactionTemplate.executeWithoutResult(status -> {
            migrateToColdStorage();
        });
    }

    /**
     * 获取迁移统计信息
     */
    public MigrationStats getMigrationStats() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);
        
        return new MigrationStats(
                activityRecordRepository.countTotalRecords(),
                activityRecordRepository.countPendingMigrationRecords(threshold),
                activityRecordRepository.countMigratingRecords()
        );
    }

    /**
     * 迁移统计信息（简化版）
     */
    public record MigrationStats(
            long totalRecords,
            long pendingRecords,
            long migratingRecords
    ) {}
}
