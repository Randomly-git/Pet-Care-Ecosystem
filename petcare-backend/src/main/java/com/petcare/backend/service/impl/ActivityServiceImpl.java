package com.petcare.backend.service.impl;

import com.petcare.backend.client.MediaServiceClient;
import com.petcare.backend.dto.response.MediaResponse;
import com.petcare.backend.entity.*;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.dto.request.CreateActivityDTO;
import com.petcare.backend.dto.request.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.ActivityService;
import com.petcare.backend.service.HBaseColdStorageService;
import com.petcare.backend.service.ColdStorageEventPublisher;
import com.petcare.backend.service.ActivityColdDataMigrationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityKindRepository activityKindRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final FixedActivityRepository fixedActivityRepository;
    private final ActivityReminderRepository activityReminderRepository;
    private final MediaServiceClient mediaServiceClient;
    
    // 冷热分离相关服务
    private final HBaseColdStorageService hBaseColdStorageService;
    private final ColdStorageEventPublisher coldStorageEventPublisher;
    private final ActivityColdDataMigrationJob activityColdDataMigrationJob;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByUserId(Long userId, Long activityKindId) {
        log.info("获取用户ID为 {} 的活动信息，活动种类ID: {}", userId, activityKindId);

        List<Activity> activities;

        if (activityKindId != null) {
            // 如果提供了活动种类ID，则按用户ID和活动种类ID筛选
            activities = activityRepository.findByUserUserIdAndActivityKindActivityKindIdAndState(userId, activityKindId, 1);
            log.debug("按用户ID和活动种类ID查询，结果数量: {}", activities.size());
        } else {
            // 如果未提供活动种类ID，则只按用户ID查询
            activities = activityRepository.findByUserUserIdAndState(userId, 1);
            log.debug("按用户ID查询，结果数量: {}", activities.size());
        }

        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByUserId(Long userId) {
        // 调用带两个参数的版本，事务仍然生效
        return getActivitiesByUserId(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActivityDTO> getActivityById(Long activityId) {
        log.info("根据ID获取有效活动信息: {}", activityId);

        return activityRepository.findByActivityIdAndState(activityId, 1)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public Activity createActivity(CreateActivityDTO createActivityDTO) {
        log.info("创建新活动: {}", createActivityDTO.getActivityName());

        // 验证用户是否存在
        User user = userRepository.findById(createActivityDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + createActivityDTO.getUserId()));

        // 验证活动种类是否存在
        ActivityKind activityKind = activityKindRepository.findById(createActivityDTO.getActivityKindId())
                .orElseThrow(() -> new RuntimeException("活动种类不存在，ID: " + createActivityDTO.getActivityKindId()));

        // 创建活动实体
        Activity activity = new Activity();
        activity.setActivityName(createActivityDTO.getActivityName());
        activity.setUser(user); // 修改为设置用户
        activity.setActivityKind(activityKind);
        activity.setState(1); // 设置为有效状态

        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public boolean deleteActivity(Long activityId) {
        log.info("软删除活动: {}", activityId);

        // 检查活动是否存在且有效
        Optional<Activity> activityOpt = activityRepository.findByActivityIdAndState(activityId, 1);
        if (activityOpt.isEmpty()) {
            log.warn("活动不存在或已被删除，ID: {}", activityId);
            return false;
        }

        int updated = activityRepository.softDelete(activityId);
        if (updated > 0) {
            log.info("活动软删除成功，ID: {}", activityId);
            return true;
        } else {
            log.error("活动软删除失败，ID: {}", activityId);
            return false;
        }
    }

    @Override
    @Transactional
    public Activity updateActivity(UpdateActivityDTO updateActivityDTO) {
        log.info("更新活动信息: {}", updateActivityDTO.getActivityId());

        // 查找有效的活动
        Activity activity = activityRepository.findByActivityIdAndState(updateActivityDTO.getActivityId(), 1)
                .orElseThrow(() -> new RuntimeException("活动不存在或已被删除，ID: " + updateActivityDTO.getActivityId()));

        // 更新活动名称（如果提供了新名称）
        if (updateActivityDTO.getActivityName() != null && !updateActivityDTO.getActivityName().trim().isEmpty()) {
            activity.setActivityName(updateActivityDTO.getActivityName());
        }

        // 更新活动种类（如果提供了新种类ID）
        if (updateActivityDTO.getActivityKindId() != null) {
            ActivityKind activityKind = activityKindRepository.findById(updateActivityDTO.getActivityKindId())
                    .orElseThrow(() -> new RuntimeException("活动种类不存在，ID: " + updateActivityDTO.getActivityKindId()));
            activity.setActivityKind(activityKind);
        }

        return activityRepository.save(activity);
    }

    // 以下方法保持不变，因为 ActivityRecord 仍然基于宠物
    @Override
    public List<ActivityKindDTO> getAllActivityKinds() {
        log.info("获取所有活动种类");

        List<ActivityKind> activityKinds = activityKindRepository.findAll();

        return activityKinds.stream()
                .map(activityKind -> {
                    ActivityKindDTO dto = new ActivityKindDTO();
                    dto.setActivityKindId(activityKind.getActivityKindId());
                    dto.setActivityKindName(activityKind.getActivityKindName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<ActivityRecordDTO> searchActivityRecords(Long petId,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         Long activityKindId,
                                                         Pageable pageable) {
        // 直接返回 Page 对象
        return activityRecordRepository.findActivityRecordsWithDetails(
                petId, startDate, endDate, activityKindId, pageable);
    }

    @Override
    @Transactional
    public boolean deleteActivityRecord(Long recordId) {
        try {
            ActivityRecord record = activityRecordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("活动记录不存在 ID=" + recordId));

            // 先删除关联的媒体文件（通过MQ异步删除COS）
            try {
                mediaServiceClient.deleteRelatedFiles("ACTIVITY", recordId);
                log.debug("删除活动记录 {} 的关联媒体成功", recordId);
            } catch (Exception e) {
                log.warn("删除活动记录 {} 的关联媒体失败，继续删除记录: {}", recordId, e.getMessage());
            }

            // 再删除记录
            activityRecordRepository.delete(record);
            log.info("已删除活动记录 ID={}", recordId);
            return true;
        } catch (Exception e) {
            log.error("删除活动记录失败 ID={}", recordId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public ActivityRecord createActivityRecord(Long petId,
                                               Long activityId,
                                               String description,
                                               LocalDateTime date) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("宠物不存在 ID=" + petId));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + activityId));

        ActivityRecord record = new ActivityRecord();
        record.setPet(pet);
        record.setActivity(activity);

        // 修改：允许描述为空
        record.setActivityDescription(description); // 如果description为null，这里会设置为null

        record.setActivityDate(date != null ? date : LocalDateTime.now());

        ActivityRecord savedRecord = activityRecordRepository.save(record);
        log.info("创建活动记录成功，记录ID: {}", savedRecord.getActivityRecordId());

        // 新增：检查并更新定时活动的提醒
        updateFixedActivityReminder(petId, activityId, savedRecord.getActivityDate());

        return savedRecord;
    }

    /**
     * 更新定时活动的提醒日期
     */
    private void updateFixedActivityReminder(Long petId, Long activityId, LocalDateTime activityDate) {
        try {
            // 查找是否存在对应的定时活动
            Optional<FixedActivity> fixedActivityOpt = fixedActivityRepository.findByPetIdAndActivityId(petId, activityId);

            if (fixedActivityOpt.isPresent()) {
                FixedActivity fixedActivity = fixedActivityOpt.get();
                LocalDate currentReminderDate = activityDate.toLocalDate();
                LocalDate newReminderDate = currentReminderDate.plusDays(fixedActivity.getGapTime());

                log.debug("找到定时活动，固定活动ID: {}, 活动ID: {}, 宠物ID: {}, 间隔天数: {}, 新提醒日期: {}",
                        fixedActivity.getFixedActivityId(), activityId, petId, fixedActivity.getGapTime(), newReminderDate);

                // 修改这里：加上 petId 查询条件
                List<ActivityReminder> reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(activityId, 1, petId);

                if (!reminders.isEmpty()) {
                    // 通常每个活动在每个宠物上应该只有一个type=1的提醒记录
                    ActivityReminder reminder = reminders.getFirst();

                    // 只有当新日期大于原提醒日期时才更新
                    if (newReminderDate.isAfter(reminder.getReminderDate())) {
                        log.info("更新活动提醒，原日期: {}, 新日期: {}, 活动ID: {}, 宠物ID: {}, 固定活动ID: {}",
                                reminder.getReminderDate(), newReminderDate, activityId, petId, fixedActivity.getFixedActivityId());
                        reminder.setReminderDate(newReminderDate);
                        activityReminderRepository.save(reminder);
                    } else {
                        log.debug("新提醒日期 {} 不大于原日期 {}，不进行更新",
                                newReminderDate, reminder.getReminderDate());
                    }
                } else {
                    // 如果没有找到现有的提醒记录，创建一个新的
                    ActivityReminder newReminder = new ActivityReminder();
                    newReminder.setActivityId(activityId);
                    newReminder.setPetId(petId);
                    newReminder.setReminderDate(newReminderDate);
                    newReminder.setType(1); // 定时活动类型

                    activityReminderRepository.save(newReminder);
                    log.info("创建新的定时活动提醒，活动ID: {}, 宠物ID: {}, 固定活动ID: {}, 提醒日期: {}",
                            activityId, petId, fixedActivity.getFixedActivityId(), newReminderDate);
                }
            } else {
                log.debug("活动ID: {} 在宠物ID: {} 上不是定时活动，无需更新提醒", activityId, petId);
            }
        } catch (Exception e) {
            log.error("更新定时活动提醒失败，活动ID: {}, 宠物ID: {}, 错误: {}",
                    activityId, petId, e.getMessage(), e);
            // 这里不抛出异常，因为主要的活动记录创建已经成功
        }
    }

    @Override
    @Transactional
    public ActivityRecord updateActivityRecord(Long recordId,
                                               Long newActivityId,
                                               String description,
                                               LocalDateTime date) {

        ActivityRecord record = activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("活动记录不存在 ID=" + recordId));

        if (newActivityId != null) {
            Activity activity = activityRepository.findById(newActivityId)
                    .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + newActivityId));
            record.setActivity(activity);
        }

        // 修改：允许描述为空，包括空字符串
        if (description != null) {
            record.setActivityDescription(description);
        } else {
            // 如果传入的description为null，设置为null
            record.setActivityDescription(null);
        }

        if (date != null) {
            record.setActivityDate(date);
        }

        return activityRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void deleteActivityCompletely(Long activityId) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + activityId));

        // 获取所有相关 record
        List<ActivityRecord> records = activityRecordRepository.findByActivityActivityId(activityId);

        // 先删除所有关联的媒体文件
        for (ActivityRecord record : records) {
            try {
                mediaServiceClient.deleteRelatedFiles("ACTIVITY", record.getActivityRecordId());
                log.debug("删除活动记录 {} 的关联媒体成功", record.getActivityRecordId());
            } catch (Exception e) {
                log.warn("删除活动记录 {} 的关联媒体失败，继续删除活动记录: {}",
                        record.getActivityRecordId(), e.getMessage());
            }
        }

        // 删除所有相关 record
        activityRecordRepository.deleteAll(records);

        // 再删除 activity
        activityRepository.delete(activity);

        log.info("彻底删除 activity={}, 以及所有 {} 条记录及其关联媒体", activityId, records.size());
    }

    /**
     * 将Activity实体转换为ActivityDTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        dto.setActivityId(activity.getActivityId());
        dto.setActivityName(activity.getActivityName());
        dto.setActivityKindId(activity.getActivityKind().getActivityKindId());
        dto.setActivityKindName(activity.getActivityKind().getActivityKindName());
        dto.setUserId(activity.getUser().getUserId()); // 修改为 userId
        dto.setUserName(activity.getUser().getName()); // 使用User实体的name字段
        dto.setState(activity.getState());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityRecordDTO> getActivityRecordsByPetIds(List<Long> petIds) {
        log.info("批量获取宠物ID: {} 的活动记录", petIds);

        if (petIds == null || petIds.isEmpty()) {
            return List.of();
        }

        // 查询这些宠物的所有活动记录
        List<ActivityRecord> records = activityRecordRepository.findByPetPetIdIn(petIds);

        return records.stream()
                .map(this::convertToRecordDTO)
                .collect(Collectors.toList());
    }

    // ==================== 冷热分离相关方法（简化版） ====================

    /**
     * 查询活动记录（自动路由热/冷数据）
     * 
     * 简化后的查询逻辑：
     * 1. 先查询 MySQL（热数据）
     * 2. 如果 MySQL 查不到，说明可能已迁移到 HBase，再查 HBase
     * 3. 合并返回结果
     * 
     * 注意：由于迁移后 MySQL 记录会被删除，所以只需要：
     * - 先查 MySQL
     * - 再查 HBase
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityRecordDTO> queryActivityRecords(Long petId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("查询活动记录（自动路由）: petId={}, startDate={}, endDate={}", petId, startDate, endDate);

        List<ActivityRecordDTO> result = new java.util.ArrayList<>();

        // 1. 查询 MySQL（热数据 + 可能还未迁移的数据）
        List<ActivityRecord> mysqlRecords = activityRecordRepository
                .findByPetPetIdAndActivityDateBetween(petId, startDate, endDate);

        for (ActivityRecord record : mysqlRecords) {
            result.add(convertToRecordDTO(record));
        }
        log.debug("查询MySQL完成: count={}", mysqlRecords.size());

        // 2. 查询 HBase（冷数据）
        // 如果 MySQL 返回数量少，可能部分数据已迁移到 HBase
        try {
            List<ActivityRecordDTO> hbaseRecords = hBaseColdStorageService
                    .queryByPetIdAndDateRange(petId, startDate, endDate);

            // 合并 HBase 结果（注意去重：HBase 可能返回已在 MySQL 中的数据）
            for (ActivityRecordDTO hbaseRecord : hbaseRecords) {
                boolean existsInMysql = mysqlRecords.stream()
                        .anyMatch(r -> r.getActivityRecordId().equals(hbaseRecord.getActivityRecordId()));
                if (!existsInMysql) {
                    result.add(hbaseRecord);
                }
            }
            log.debug("查询HBase完成: count={}", hbaseRecords.size());
        } catch (Exception e) {
            log.error("查询HBase冷数据失败: petId={}, error={}", petId, e.getMessage());
            // 冷数据查询失败不影响热数据返回
        }

        // 3. 按日期排序
        result.sort((a, b) -> {
            if (a.getActivityDate() == null || b.getActivityDate() == null) return 0;
            return a.getActivityDate().compareTo(b.getActivityDate());
        });

        log.info("查询活动记录完成: petId={}, totalCount={}", petId, result.size());
        return result;
    }

    /**
     * 访问冷数据记录（触发解冻）
     * 
     * 简化后的逻辑：
     * 由于迁移后 MySQL 记录会被删除，此方法主要处理：
     * 1. 检查 MySQL 中是否存在记录
     * 2. 如果存在（正在迁移中或迁移失败），检查解冻状态
     * 3. 如果不存在，说明已迁移，查询 HBase 并设置解冻过期时间
     */
    @Override
    @Transactional
    public void accessColdRecord(Long activityRecordId) {
        log.info("访问活动记录: activityRecordId={}", activityRecordId);

        ActivityRecord record = activityRecordRepository.findById(activityRecordId).orElse(null);
        
        if (record == null) {
            // MySQL 中不存在，可能是已迁移到 HBase 的数据
            log.info("MySQL中无记录，可能是冷数据，生成RowKey检查HBase: activityRecordId={}", activityRecordId);
            
            // 这里需要通过其他方式获取 petId 和 activityDate 来生成 RowKey
            // 由于简化设计，我们无法从 MySQL 获取这些信息
            // 因此需要外部传入或在消息中携带这些信息
            // 这里暂时只记录日志，实际解冻逻辑由 MQ 消费者处理
            return;
        }

        // 检查解冻过期时间
        LocalDateTime now = LocalDateTime.now();
        if (record.getThawExpireTime() != null && now.isBefore(record.getThawExpireTime())) {
            // 仍在解冻有效期内，只更新时间戳
            log.info("访问仍在解冻有效期内，刷新过期时间: activityRecordId={}", activityRecordId);
            record.setThawExpireTime(now.plusMinutes(10));
            activityRecordRepository.save(record);
            return;
        }

        // 不在有效期内，需要重新解冻
        log.info("需要解冻: activityRecordId={}", activityRecordId);
        coldStorageEventPublisher.publishThawFromColdEvent(
                record.getActivityRecordId(),
                record.getPet() != null ? record.getPet().getPetId() : null,
                record.getActivity() != null ? record.getActivity().getActivityId() : null,
                record.getActivityDate()
        );
    }

    /**
     * 获取迁移统计信息
     */
    @Override
    public Object getMigrationStats() {
        return activityColdDataMigrationJob.getMigrationStats();
    }

    /**
     * 将ActivityRecord实体转换为ActivityRecordDTO
     */
    private ActivityRecordDTO convertToRecordDTO(ActivityRecord record) {
        ActivityRecordDTO dto = new ActivityRecordDTO();
        dto.setActivityRecordId(record.getActivityRecordId());
        dto.setActivityId(record.getActivity() != null ? record.getActivity().getActivityId() : null);
        dto.setActivityName(record.getActivity() != null ? record.getActivity().getActivityName() : null);
        dto.setActivityKindId(record.getActivity() != null && record.getActivity().getActivityKind() != null 
                ? record.getActivity().getActivityKind().getActivityKindId() : null);
        dto.setActivityKindName(record.getActivity() != null && record.getActivity().getActivityKind() != null 
                ? record.getActivity().getActivityKind().getActivityKindName() : null);
        dto.setPetId(record.getPet() != null ? record.getPet().getPetId() : null);
        dto.setPetName(record.getPet() != null ? record.getPet().getName() : null);
        dto.setActivityDescription(record.getActivityDescription());
        dto.setActivityDate(record.getActivityDate());
        return dto;
    }
}