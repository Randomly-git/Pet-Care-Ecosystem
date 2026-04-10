package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreateFixedActivityDTO;
import com.petcare.backend.dto.request.UpdateFixedActivityDTO;
import com.petcare.backend.dto.response.FixedActivityDTO;
import com.petcare.backend.entity.*;
import com.petcare.backend.repository.*;
import com.petcare.backend.service.FixedActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedActivityServiceImpl implements FixedActivityService {

    private final FixedActivityRepository fixedActivityRepository;
    private final ActivityReminderRepository activityReminderRepository;
    private final ActivityRepository activityRepository;
    private final PetRepository petRepository;
    private final ActivityKindRepository activityKindRepository;

    @Override
    @Transactional
    public FixedActivity createFixedActivity(CreateFixedActivityDTO createFixedActivityDTO) {
        log.info("创建定时活动，宠物ID: {}, 活动ID: {}, 间隔天数: {}",
                createFixedActivityDTO.getPetId(), createFixedActivityDTO.getActivityId(), createFixedActivityDTO.getGapTime());

        // 验证宠物是否存在
        Pet pet = petRepository.findById(createFixedActivityDTO.getPetId())
                .orElseThrow(() -> new RuntimeException("宠物不存在，ID: " + createFixedActivityDTO.getPetId()));

        // 验证活动是否存在
        Activity activity = activityRepository.findById(createFixedActivityDTO.getActivityId())
                .orElseThrow(() -> new RuntimeException("活动不存在，ID: " + createFixedActivityDTO.getActivityId()));

        // 检查同一个宠物下是否已经存在相同活动的定时设置
        boolean exists = fixedActivityRepository.findByPetIdAndActivityId(
                createFixedActivityDTO.getPetId(), createFixedActivityDTO.getActivityId()).isPresent();

        if (exists) {
            throw new RuntimeException("该宠物下已经存在相同活动的定时设置");
        }

        // 创建 FixedActivity
        FixedActivity fixedActivity = new FixedActivity();
        fixedActivity.setPetId(createFixedActivityDTO.getPetId());
        fixedActivity.setActivityId(createFixedActivityDTO.getActivityId());
        fixedActivity.setGapTime(createFixedActivityDTO.getGapTime());

        FixedActivity savedFixedActivity = fixedActivityRepository.save(fixedActivity);
        log.info("定时活动创建成功，ID: {}", savedFixedActivity.getFixedActivityId());

        // 创建对应的 ActivityReminder
        createActivityReminderForFixedActivity(savedFixedActivity);

        return savedFixedActivity;
    }

    @Override
    @Transactional
    public FixedActivity updateFixedActivityGapTime(UpdateFixedActivityDTO updateFixedActivityDTO) {
        log.info("修改定时活动间隔时间，ID: {}, 新间隔天数: {}",
                updateFixedActivityDTO.getFixedActivityId(), updateFixedActivityDTO.getGapTime());

        // 查找 FixedActivity
        FixedActivity fixedActivity = fixedActivityRepository.findById(updateFixedActivityDTO.getFixedActivityId())
                .orElseThrow(() -> new RuntimeException("定时活动不存在，ID: " + updateFixedActivityDTO.getFixedActivityId()));

        // 保存旧的 gapTime 用于比较
        Integer oldGapTime = fixedActivity.getGapTime();

        // 更新间隔时间
        fixedActivity.setGapTime(updateFixedActivityDTO.getGapTime());
        FixedActivity updatedFixedActivity = fixedActivityRepository.save(fixedActivity);

        // 更新对应的 ActivityReminder，传入旧的 gapTime
        updateActivityReminderForFixedActivity(updatedFixedActivity, oldGapTime);

        log.info("定时活动间隔时间更新成功，ID: {}", updatedFixedActivity.getFixedActivityId());
        return updatedFixedActivity;
    }

    // 修改 getFixedActivitiesByPetId 方法，移除后续的提醒日期设置逻辑
    @Override
    @Transactional(readOnly = true)
    public List<FixedActivityDTO> getFixedActivitiesByPetId(Long petId) {
        log.info("查询宠物ID: {} 的所有定时活动", petId);

        // 验证宠物是否存在
        if (!petRepository.existsById(petId)) {
            throw new RuntimeException("宠物不存在，ID: " + petId);
        }

        // 使用 JOIN 查询获取完整信息（包含提醒日期）
        List<Object[]> results = fixedActivityRepository.findFixedActivitiesWithDetailsByPetId(petId);

        log.info("JOIN 查询结果数量: " + results.size());

        // 手动转换Object[]到DTO（已经包含提醒日期）
        List<FixedActivityDTO> dtos = results.stream()
                .map(this::convertToDTOFromNativeResult)
                .collect(Collectors.toList());

        return dtos;
    }

    @Override
    @Transactional
    public void deleteFixedActivity(Long fixedActivityId) {
        log.info("删除定时活动，ID: {}", fixedActivityId);

        // 查找 FixedActivity
        FixedActivity fixedActivity = fixedActivityRepository.findById(fixedActivityId)
                .orElseThrow(() -> new RuntimeException("定时活动不存在，ID: " + fixedActivityId));

        // 删除对应的 ActivityReminder
        deleteActivityReminderForFixedActivity(fixedActivity);

        // 删除 FixedActivity
        fixedActivityRepository.delete(fixedActivity);

        log.info("定时活动删除成功，ID: {}", fixedActivityId);
    }

    /**
     * 为 FixedActivity 创建 ActivityReminder
     */
    private void createActivityReminderForFixedActivity(FixedActivity fixedActivity) {
        LocalDate reminderDate = LocalDate.now().plusDays(fixedActivity.getGapTime());

        ActivityReminder activityReminder = new ActivityReminder();
        activityReminder.setActivityId(fixedActivity.getActivityId());
        activityReminder.setPetId(fixedActivity.getPetId());
        activityReminder.setReminderDate(reminderDate);
        activityReminder.setType(1); // 定时活动类型

        ActivityReminder savedReminder = activityReminderRepository.save(activityReminder);
        log.info("创建定时活动提醒成功，提醒ID: {}, 活动ID: {}, 宠物ID: {}, 提醒日期: {}",
                savedReminder.getActivityReminderId(), fixedActivity.getActivityId(),
                fixedActivity.getPetId(), reminderDate);
    }

    /**
     * 更新 FixedActivity 对应的 ActivityReminder
     */
    private void updateActivityReminderForFixedActivity(FixedActivity fixedActivity, Integer oldGapTime) {
        // 查找对应的 ActivityReminder
        List<ActivityReminder> reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                fixedActivity.getActivityId(), 1, fixedActivity.getPetId());

        if (!reminders.isEmpty()) {
            ActivityReminder reminder = reminders.get(0);

            // 计算 gapTime 的变化量
            int gapTimeChange = fixedActivity.getGapTime() - oldGapTime;

            // 根据变化量调整提醒日期
            LocalDate newReminderDate = reminder.getReminderDate().plusDays(gapTimeChange);

            reminder.setReminderDate(newReminderDate);
            activityReminderRepository.save(reminder);

            log.info("更新定时活动提醒成功，提醒ID: {}, 原提醒日期: {}, 新提醒日期: {}, gapTime变化量: {} (旧gapTime: {} -> 新gapTime: {})",
                    reminder.getActivityReminderId(), reminder.getReminderDate(), newReminderDate,
                    gapTimeChange, oldGapTime, fixedActivity.getGapTime());
        } else {
            // 如果没有找到提醒记录，创建新的
            createActivityReminderForFixedActivity(fixedActivity);
        }
    }

    /**
     * 删除 FixedActivity 对应的 ActivityReminder
     */
    private void deleteActivityReminderForFixedActivity(FixedActivity fixedActivity) {
        List<ActivityReminder> reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                fixedActivity.getActivityId(), 1, fixedActivity.getPetId());

        if (!reminders.isEmpty()) {
            activityReminderRepository.deleteAll(reminders);
            log.info("删除定时活动提醒成功，活动ID: {}, 宠物ID: {}, 删除记录数: {}",
                    fixedActivity.getActivityId(), fixedActivity.getPetId(), reminders.size());
        }
    }

    /// 修改 DTO 转换方法
    private FixedActivityDTO convertToDTOFromNativeResult(Object[] result) {
        // 安全类型转换
        Long fixedActivityId = convertToLong(result[0]);
        Long activityId = convertToLong(result[1]);
        Long petId = convertToLong(result[2]);
        Integer gapTime = convertToInteger(result[3]);
        String activityName = (String) result[4];
        Long activityKindId = convertToLong(result[5]);
        String activityKindName = (String) result[6];
        String petName = (String) result[7];
        LocalDate nextReminderDate = convertToLocalDate(result[8]);  // 新增 reminder_date 转换

        FixedActivityDTO dto = new FixedActivityDTO();
        dto.setFixedActivityId(fixedActivityId);
        dto.setActivityId(activityId);
        dto.setPetId(petId);
        dto.setGapTime(gapTime);
        dto.setActivityName(activityName);
        dto.setActivityKindId(activityKindId);
        dto.setActivityKindName(activityKindName);
        dto.setPetName(petName);
        dto.setNextReminderDate(nextReminderDate);  // 直接设置 nextReminderDate

        return dto;
    }

    // 新增 LocalDate 转换方法
    private LocalDate convertToLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) value).getTime()).toLocalDateTime().toLocalDate();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to LocalDate");
    }

    // 安全类型转换工具方法
    private Long convertToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Long");
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Integer");
    }
}