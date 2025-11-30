package com.petcare.backend.service.impl;

import com.petcare.backend.dto.response.ReminderDTO;
import com.petcare.backend.entity.ActivityReminder;
import com.petcare.backend.repository.ActivityReminderRepository;
import com.petcare.backend.service.ActivityService;
import com.petcare.backend.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private final ActivityReminderRepository activityReminderRepository;
    private final ActivityService activityService;

    @Override
    @Transactional(readOnly = true)
    public List<ReminderDTO> getOverdueRemindersByPetId(Long petId) {
        log.info("获取宠物ID: {} 的过期提醒", petId);

        // 使用原生SQL查询获取过期提醒及活动名称
        List<Object[]> results = activityReminderRepository.findOverdueRemindersByPetId(petId);

        log.info("过期提醒查询结果数量: " + results.size());

        // 转换Object[]到DTO
        return results.stream()
                .map(this::convertToReminderDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void postponeReminder(Long activityReminderId) {
        log.info("暂时忽略提醒，提醒ID: {}", activityReminderId);

        // 查找提醒记录
        ActivityReminder activityReminder = activityReminderRepository.findById(activityReminderId)
                .orElseThrow(() -> new RuntimeException("提醒记录不存在，ID: " + activityReminderId));

        // 将提醒日期推迟一天
        LocalDate newReminderDate = activityReminder.getReminderDate().plusDays(1);
        activityReminder.setReminderDate(newReminderDate);
        activityReminderRepository.save(activityReminder);

        log.info("提醒日期推迟成功，提醒ID: {}, 新提醒日期: {}",
                activityReminderId, newReminderDate);
    }

    @Override
    @Transactional
    public void confirmReminderWithoutDescription(Long activityReminderId) {
        log.info("确认提醒（无描述），提醒ID: {}", activityReminderId);
        confirmReminder(activityReminderId, null);
    }

    @Override
    @Transactional
    public void confirmReminderWithDescription(Long activityReminderId, String description) {
        log.info("确认提醒（有描述），提醒ID: {}, 描述: {}", activityReminderId, description);
        confirmReminder(activityReminderId, description);
    }

    /**
     * 确认提醒的通用方法
     */
    private void confirmReminder(Long activityReminderId, String description) {
        // 查找提醒记录
        ActivityReminder activityReminder = activityReminderRepository.findById(activityReminderId)
                .orElseThrow(() -> new RuntimeException("提醒记录不存在，ID: " + activityReminderId));

        Long activityId = activityReminder.getActivityId();
        Long petId = activityReminder.getPetId();
        Integer type = activityReminder.getType();

        log.info("确认提醒处理，提醒ID: {}, 活动ID: {}, 宠物ID: {}, 类型: {}",
                activityReminderId, activityId, petId, type);

        // 创建活动记录（使用ActivityService中的方法）
        activityService.createActivityRecord(petId, activityId, description, null);

        log.info("活动记录创建成功，提醒ID: {}, 活动ID: {}, 宠物ID: {}",
                activityReminderId, activityId, petId);

        // 如果是type=2（一次性提醒），删除提醒记录
        if (type.equals(2)) {
            activityReminderRepository.delete(activityReminder);
            log.info("一次性提醒已删除，提醒ID: {}", activityReminderId);
        } else {
            log.info("定时提醒（type=1）保留，提醒ID: {}", activityReminderId);
        }
    }

    /**
     * 转换原生SQL查询结果到ReminderDTO
     */
    private ReminderDTO convertToReminderDTO(Object[] result) {
        Long activityReminderId = convertToLong(result[0]);
        Long activityId = convertToLong(result[1]);
        Long petId = convertToLong(result[2]);
        LocalDate reminderDate = convertToLocalDate(result[3]);
        Integer type = convertToInteger(result[4]);
        String activityName = (String) result[5];

        return new ReminderDTO(activityReminderId, activityId, petId, reminderDate, type, activityName);
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

    private LocalDate convertToLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) value).getTime()).toLocalDateTime().toLocalDate();
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to LocalDate");
    }
}