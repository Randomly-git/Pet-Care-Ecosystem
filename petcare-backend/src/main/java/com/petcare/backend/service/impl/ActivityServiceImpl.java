// ActivityServiceImpl.java (更新后)
package com.petcare.backend.service.impl;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.dto.response.CreateActivityDTO;
import com.petcare.backend.dto.response.UpdateActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.ActivityRecordRepository;
import com.petcare.backend.repository.ActivityKindRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityKindRepository activityKindRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByPetId(Long petId, Long activityKindId) {
        log.info("获取宠物ID为 {} 的活动信息，活动种类ID: {}", petId, activityKindId);

        List<Activity> activities;

        if (activityKindId != null) {
            // 如果提供了活动种类ID，则按宠物ID和活动种类ID筛选
            activities = activityRepository.findByPetPetIdAndActivityKindActivityKindIdAndState(petId, activityKindId, 1);
            log.debug("按宠物ID和活动种类ID查询，结果数量: {}", activities.size());
        } else {
            // 如果未提供活动种类ID，则只按宠物ID查询
            activities = activityRepository.findByPetPetIdAndState(petId, 1);
            log.debug("按宠物ID查询，结果数量: {}", activities.size());
        }

        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivitiesByPetId(Long petId) {
        // 调用带两个参数的版本，事务仍然生效
        return getActivitiesByPetId(petId, null);
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

        // 验证宠物是否存在
        Pet pet = petRepository.findById(createActivityDTO.getPetId())
                .orElseThrow(() -> new RuntimeException("宠物不存在，ID: " + createActivityDTO.getPetId()));

        // 验证活动种类是否存在
        ActivityKind activityKind = activityKindRepository.findById(createActivityDTO.getActivityKindId())
                .orElseThrow(() -> new RuntimeException("活动种类不存在，ID: " + createActivityDTO.getActivityKindId()));

        // 创建活动实体
        Activity activity = new Activity();
        activity.setActivityName(createActivityDTO.getActivityName());
        activity.setPet(pet);
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

    @Override
    @Transactional(readOnly = true)
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

    /**
     * 1. 根据 pet_id 搜索活动记录，支持日期范围 & activity_kind_id 可选过滤
     */
    @Override
    public List<ActivityRecordDTO> searchActivityRecords(Long petId,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         Long activityKindId) {
        return activityRecordRepository.findActivityRecordsWithDetails(petId, startDate, endDate, activityKindId);
    }

    /**
     * 2. 删除活动记录（硬删除）
     */
    @Override
    @Transactional
    public void deleteActivityRecord(Long recordId) {
        ActivityRecord record = activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("活动记录不存在 ID=" + recordId));

        activityRecordRepository.delete(record);
        log.info("已删除活动记录 ID={}", recordId);
    }

    /**
     * 3. 插入新的 ActivityRecord
     */
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
        record.setActivityDescription(description);
        record.setActivityDate(date != null ? date : LocalDateTime.now());

        return activityRecordRepository.save(record);
    }

    /**
     * 4. 修改活动记录
     */
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

        if (description != null) {
            record.setActivityDescription(description);
        }

        if (date != null) {
            record.setActivityDate(date);
        }

        return activityRecordRepository.save(record);
    }

    /**
     * 5. 完全删除一个 Activity（先删记录，再删 Activity）
     */
    @Override
    @Transactional
    public void deleteActivityCompletely(Long activityId) {

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("活动不存在 ID=" + activityId));

        // 删除所有相关 record
        List<ActivityRecord> records = activityRecordRepository.findByActivityActivityId(activityId);
        activityRecordRepository.deleteAll(records);

        // 再删除 activity
        activityRepository.delete(activity);

        log.info("彻底删除 activity={}, 以及所有 {} 条记录", activityId, records.size());
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
        dto.setPetId(activity.getPet().getPetId());
        dto.setPetName(activity.getPet().getName()); // 使用Pet实体的name字段
        dto.setState(activity.getState());

        return dto;
    }
}