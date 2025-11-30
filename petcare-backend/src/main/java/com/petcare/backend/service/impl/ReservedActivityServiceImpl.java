package com.petcare.backend.service.impl;

import com.petcare.backend.dto.request.CreateReservedActivityDTO;
import com.petcare.backend.dto.request.UpdateReservedActivityDTO;
import com.petcare.backend.dto.response.ReservedActivityDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityReminder;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.repository.ActivityReminderRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.service.ReservedActivityService;
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
public class ReservedActivityServiceImpl implements ReservedActivityService {

    private final ActivityReminderRepository activityReminderRepository;
    private final ActivityRepository activityRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public ActivityReminder createReservedActivity(CreateReservedActivityDTO createReservedActivityDTO) {
        log.info("创建一次性提醒，宠物ID: {}, 活动ID: {}, 提醒日期: {}",
                createReservedActivityDTO.getPetId(), createReservedActivityDTO.getActivityId(),
                createReservedActivityDTO.getReminderDate());

        // 验证宠物是否存在
        Pet pet = petRepository.findById(createReservedActivityDTO.getPetId())
                .orElseThrow(() -> new RuntimeException("宠物不存在，ID: " + createReservedActivityDTO.getPetId()));

        // 验证活动是否存在
        Activity activity = activityRepository.findById(createReservedActivityDTO.getActivityId())
                .orElseThrow(() -> new RuntimeException("活动不存在，ID: " + createReservedActivityDTO.getActivityId()));

        // 验证提醒日期不能早于今天
        if (createReservedActivityDTO.getReminderDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("提醒日期不能早于今天");
        }

        // 创建一次性提醒
        ActivityReminder activityReminder = new ActivityReminder();
        activityReminder.setActivityId(createReservedActivityDTO.getActivityId());
        activityReminder.setPetId(createReservedActivityDTO.getPetId());
        activityReminder.setReminderDate(createReservedActivityDTO.getReminderDate());
        activityReminder.setType(2); // 一次性提醒类型

        ActivityReminder savedReminder = activityReminderRepository.save(activityReminder);
        log.info("一次性提醒创建成功，提醒ID: {}, 活动ID: {}, 宠物ID: {}, 提醒日期: {}",
                savedReminder.getActivityReminderId(), savedReminder.getActivityId(),
                savedReminder.getPetId(), savedReminder.getReminderDate());

        return savedReminder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservedActivityDTO> getReservedActivitiesByPetId(Long petId) {
        log.info("查询宠物ID: {} 的所有一次性提醒", petId);

        // 验证宠物是否存在
        if (!petRepository.existsById(petId)) {
            throw new RuntimeException("宠物不存在，ID: " + petId);
        }

        // 使用原生SQL查询获取一次性提醒及活动名称
        List<Object[]> results = activityReminderRepository.findReservedActivitiesWithDetailsByPetId(petId);

        log.info("一次性提醒查询结果数量: " + results.size());

        // 转换Object[]到DTO
        return results.stream()
                .map(this::convertToReservedActivityDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ActivityReminder updateReservedActivityDate(UpdateReservedActivityDTO updateReservedActivityDTO) {
        log.info("修改一次性提醒日期，提醒ID: {}, 新日期: {}",
                updateReservedActivityDTO.getActivityReminderId(), updateReservedActivityDTO.getReminderDate());

        // 查找一次性提醒记录
        ActivityReminder activityReminder = activityReminderRepository.findById(updateReservedActivityDTO.getActivityReminderId())
                .orElseThrow(() -> new RuntimeException("提醒记录不存在，ID: " + updateReservedActivityDTO.getActivityReminderId()));

        // 检查类型是否为一次性提醒
        if (!activityReminder.getType().equals(2)) {
            throw new RuntimeException("只能修改一次性提醒的日期");
        }

        // 验证提醒日期不能早于今天
        if (updateReservedActivityDTO.getReminderDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("提醒日期不能早于今天");
        }

        // 更新提醒日期
        activityReminder.setReminderDate(updateReservedActivityDTO.getReminderDate());
        ActivityReminder updatedReminder = activityReminderRepository.save(activityReminder);

        log.info("一次性提醒日期更新成功，提醒ID: {}, 新日期: {}",
                updatedReminder.getActivityReminderId(), updatedReminder.getReminderDate());

        return updatedReminder;
    }

    @Override
    @Transactional
    public void deleteReservedActivity(Long activityReminderId) {
        log.info("删除一次性提醒，ID: {}", activityReminderId);

        // 查找提醒记录
        ActivityReminder activityReminder = activityReminderRepository.findById(activityReminderId)
                .orElseThrow(() -> new RuntimeException("提醒记录不存在，ID: " + activityReminderId));

        // 检查类型是否为一次性提醒
        if (!activityReminder.getType().equals(2)) {
            throw new RuntimeException("只能删除一次性提醒");
        }

        // 删除提醒记录
        activityReminderRepository.delete(activityReminder);

        log.info("一次性提醒删除成功，ID: {}", activityReminderId);
    }

    /**
     * 转换原生SQL查询结果到ReservedActivityDTO
     */
    private ReservedActivityDTO convertToReservedActivityDTO(Object[] result) {
        Long activityReminderId = convertToLong(result[0]);
        Long activityId = convertToLong(result[1]);
        Long petId = convertToLong(result[2]);
        LocalDate reminderDate = convertToLocalDate(result[3]);
        String activityName = (String) result[4];

        return new ReservedActivityDTO(activityReminderId, activityId, petId, reminderDate, activityName);
    }

    // 安全类型转换工具方法
    private Long convertToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Long");
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