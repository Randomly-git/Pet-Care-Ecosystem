package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateReservedActivityDTO;
import com.petcare.backend.dto.request.UpdateReservedActivityDTO;
import com.petcare.backend.dto.response.ReservedActivityDTO;
import com.petcare.backend.entity.ActivityReminder;
import com.petcare.backend.repository.ActivityReminderRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.service.impl.ReservedActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ReservedActivityServiceImplTest {

    @Autowired
    private ReservedActivityServiceImpl reservedActivityService;

    @Autowired
    private ActivityReminderRepository activityReminderRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PetRepository petRepository;

    // 测试参数 - 由你统一控制这些值
    private Long existingPetId;
    private Long existingActivityId;
    private Long nonExistingPetId = 9999L;
    private Long nonExistingActivityId = 9999L;
    private LocalDate validReminderDate;
    private LocalDate pastReminderDate;

    @BeforeEach
    void setUp() {
        // 请根据你的dev环境数据库设置这些参数
        existingPetId = 393L; // 替换为实际存在的宠物ID
        existingActivityId = 274L; // 替换为实际存在的活动ID
        validReminderDate = LocalDate.now().plusDays(7);
        pastReminderDate = LocalDate.now().minusDays(1);
    }

    @Test
    void createReservedActivity_Success() {
        // 准备
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);

        // 执行
        ActivityReminder result = reservedActivityService.createReservedActivity(createDTO);

        // 验证
        assertNotNull(result);
        assertNotNull(result.getActivityReminderId());
        assertEquals(existingActivityId, result.getActivityId());
        assertEquals(existingPetId, result.getPetId());
        assertEquals(validReminderDate, result.getReminderDate());
        assertEquals(2, result.getType()); // 一次性提醒类型

        // 验证数据库中存在该记录
        assertTrue(activityReminderRepository.findById(result.getActivityReminderId()).isPresent());
    }

    @Test
    void createReservedActivity_PetNotFound() {
        // 准备
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(nonExistingPetId);
        createDTO.setReminderDate(validReminderDate);

        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.createReservedActivity(createDTO));
        assertTrue(exception.getMessage().contains("宠物不存在"));
    }

    @Test
    void createReservedActivity_ActivityNotFound() {
        // 准备
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(nonExistingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);

        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.createReservedActivity(createDTO));
        assertTrue(exception.getMessage().contains("活动不存在"));
    }

    @Test
    void createReservedActivity_PastDate() {
        // 准备
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(pastReminderDate);

        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.createReservedActivity(createDTO));
        assertTrue(exception.getMessage().contains("提醒日期不能早于今天"));
    }

    @Test
    void getReservedActivitiesByPetId_Success() {
        // 先创建一个一次性提醒
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);
        ActivityReminder created = reservedActivityService.createReservedActivity(createDTO);

        // 执行查询
        List<ReservedActivityDTO> results = reservedActivityService.getReservedActivitiesByPetId(existingPetId);

        // 验证
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证返回的DTO包含正确的信息
        ReservedActivityDTO dto = results.stream()
                .filter(r -> r.getActivityReminderId().equals(created.getActivityReminderId()))
                .findFirst()
                .orElse(null);

        assertNotNull(dto);
        assertEquals(created.getActivityReminderId(), dto.getActivityReminderId());
        assertEquals(existingActivityId, dto.getActivityId());
        assertEquals(existingPetId, dto.getPetId());
        assertEquals(validReminderDate, dto.getReminderDate());
        assertNotNull(dto.getActivityName()); // 应该包含活动名称
    }

    @Test
    void getReservedActivitiesByPetId_PetNotFound() {
        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.getReservedActivitiesByPetId(nonExistingPetId));
        assertTrue(exception.getMessage().contains("宠物不存在"));
    }

    @Test
    void updateReservedActivityDate_Success() {
        // 先创建一个一次性提醒
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);
        ActivityReminder created = reservedActivityService.createReservedActivity(createDTO);

        // 准备更新数据
        LocalDate newDate = validReminderDate.plusDays(3);
        UpdateReservedActivityDTO updateDTO = new UpdateReservedActivityDTO();
        updateDTO.setActivityReminderId(created.getActivityReminderId());
        updateDTO.setReminderDate(newDate);

        // 执行更新
        ActivityReminder updated = reservedActivityService.updateReservedActivityDate(updateDTO);

        // 验证
        assertNotNull(updated);
        assertEquals(newDate, updated.getReminderDate());
        assertEquals(created.getActivityReminderId(), updated.getActivityReminderId());
        assertEquals(2, updated.getType()); // 类型保持不变

        // 验证数据库已更新
        ActivityReminder fromDb = activityReminderRepository.findById(created.getActivityReminderId()).orElseThrow();
        assertEquals(newDate, fromDb.getReminderDate());
    }

    @Test
    void updateReservedActivityDate_NotFound() {
        // 准备
        UpdateReservedActivityDTO updateDTO = new UpdateReservedActivityDTO();
        updateDTO.setActivityReminderId(9999L); // 不存在的ID
        updateDTO.setReminderDate(validReminderDate);

        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.updateReservedActivityDate(updateDTO));
        assertTrue(exception.getMessage().contains("提醒记录不存在"));
    }

    @Test
    void updateReservedActivityDate_PastDate() {
        // 先创建一个一次性提醒
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);
        ActivityReminder created = reservedActivityService.createReservedActivity(createDTO);

        // 准备更新数据（过去日期）
        UpdateReservedActivityDTO updateDTO = new UpdateReservedActivityDTO();
        updateDTO.setActivityReminderId(created.getActivityReminderId());
        updateDTO.setReminderDate(pastReminderDate);

        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.updateReservedActivityDate(updateDTO));
        assertTrue(exception.getMessage().contains("提醒日期不能早于今天"));
    }

    @Test
    void deleteReservedActivity_Success() {
        // 先创建一个一次性提醒
        CreateReservedActivityDTO createDTO = new CreateReservedActivityDTO();
        createDTO.setActivityId(existingActivityId);
        createDTO.setPetId(existingPetId);
        createDTO.setReminderDate(validReminderDate);
        ActivityReminder created = reservedActivityService.createReservedActivity(createDTO);

        Long reminderId = created.getActivityReminderId();

        // 验证记录存在
        assertTrue(activityReminderRepository.findById(reminderId).isPresent());

        // 执行删除
        reservedActivityService.deleteReservedActivity(reminderId);

        // 验证记录已删除
        assertFalse(activityReminderRepository.findById(reminderId).isPresent());
    }

    @Test
    void deleteReservedActivity_NotFound() {
        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservedActivityService.deleteReservedActivity(9999L));
        assertTrue(exception.getMessage().contains("提醒记录不存在"));
    }
}