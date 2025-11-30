package com.petcare.backend.service;

import com.petcare.backend.dto.response.ReminderDTO;
import com.petcare.backend.entity.ActivityReminder;
import com.petcare.backend.repository.ActivityReminderRepository;
import com.petcare.backend.repository.ActivityRecordRepository;
import com.petcare.backend.service.impl.ReminderServiceImpl;
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
class ReminderServiceImplTest {

    @Autowired
    private ReminderServiceImpl reminderService;

    @Autowired
    private ActivityReminderRepository activityReminderRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    // 测试参数 - 由你统一控制这些值
    private Long existingPetId;
    private Long existingActivityId;
    private Long nonExistingPetId = 9999L;
    private Long nonExistingReminderId = 9999L;
    private Long type1ReminderId; // 定时活动提醒ID
    private Long type2ReminderId; // 一次性提醒ID

    @BeforeEach
    void setUp() {
        // 请根据你的dev环境数据库设置这些参数
        existingPetId = 393L; // 替换为实际存在的宠物ID
        existingActivityId = 315L; // 替换为实际存在的活动ID

        // 创建测试数据
        createTestReminders();
    }

    private void createTestReminders() {
        // 创建过期的一次性提醒 (type=2)
        ActivityReminder type2Reminder = new ActivityReminder();
        type2Reminder.setActivityId(existingActivityId);
        type2Reminder.setPetId(existingPetId);
        type2Reminder.setReminderDate(LocalDate.now().minusDays(1)); // 过期日期
        type2Reminder.setType(2);
        ActivityReminder savedType2 = activityReminderRepository.save(type2Reminder);
        type2ReminderId = savedType2.getActivityReminderId();

        // 创建过期的定时活动提醒 (type=1)
        ActivityReminder type1Reminder = new ActivityReminder();
        type1Reminder.setActivityId(existingActivityId);
        type1Reminder.setPetId(existingPetId);
        type1Reminder.setReminderDate(LocalDate.now().minusDays(2)); // 过期日期
        type1Reminder.setType(1);
        ActivityReminder savedType1 = activityReminderRepository.save(type1Reminder);
        type1ReminderId = savedType1.getActivityReminderId();

        // 创建未来的提醒（不应在结果中）
        ActivityReminder futureReminder = new ActivityReminder();
        futureReminder.setActivityId(existingActivityId);
        futureReminder.setPetId(existingPetId);
        futureReminder.setReminderDate(LocalDate.now().plusDays(1)); // 未来日期
        futureReminder.setType(2);
        activityReminderRepository.save(futureReminder);
    }

    @Test
    void getOverdueRemindersByPetId_Success() {
        // 执行
        List<ReminderDTO> results = reminderService.getOverdueRemindersByPetId(existingPetId);

        // 验证
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证返回的DTO包含正确的信息
        boolean foundType1 = results.stream()
                .anyMatch(r -> r.getActivityReminderId().equals(type1ReminderId) && r.getType().equals(1));
        boolean foundType2 = results.stream()
                .anyMatch(r -> r.getActivityReminderId().equals(type2ReminderId) && r.getType().equals(2));

        assertTrue(foundType1, "应该包含type=1的提醒");
        assertTrue(foundType2, "应该包含type=2的提醒");

        // 验证每个DTO都包含活动名称
        results.forEach(dto -> {
            assertNotNull(dto.getActivityName());
            assertNotNull(dto.getActivityId());
            assertNotNull(dto.getPetId());
            assertNotNull(dto.getReminderDate());
            assertNotNull(dto.getType());
            assertTrue(dto.getReminderDate().isBefore(LocalDate.now()), "提醒日期应该早于今天");
        });
    }

    @Test
    void getOverdueRemindersByPetId_NoResults() {
        // 使用不存在的宠物ID
        List<ReminderDTO> results = reminderService.getOverdueRemindersByPetId(nonExistingPetId);

        // 验证
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void postponeReminder_Success() {
        // 准备
        ActivityReminder originalReminder = activityReminderRepository.findById(type1ReminderId).orElseThrow();
        LocalDate originalDate = originalReminder.getReminderDate();

        // 执行
        reminderService.postponeReminder(type1ReminderId);

        // 验证
        ActivityReminder updatedReminder = activityReminderRepository.findById(type1ReminderId).orElseThrow();
        LocalDate newDate = updatedReminder.getReminderDate();

        assertEquals(originalDate.plusDays(1), newDate, "提醒日期应该推迟一天");
    }

    @Test
    void postponeReminder_NotFound() {
        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reminderService.postponeReminder(nonExistingReminderId));
        assertTrue(exception.getMessage().contains("提醒记录不存在"));
    }

    @Test
    void confirmReminderWithoutDescription_Type2_Success() {
        // 准备 - 记录确认前的状态
        long initialRecordCount = activityRecordRepository.count();
        boolean reminderExistsBefore = activityReminderRepository.findById(type2ReminderId).isPresent();

        // 执行
        reminderService.confirmReminderWithoutDescription(type2ReminderId);

        // 验证
        // 1. 活动记录应该被创建
        long finalRecordCount = activityRecordRepository.count();
        assertEquals(initialRecordCount + 1, finalRecordCount, "应该创建一个活动记录");

        // 2. type=2的提醒应该被删除
        boolean reminderExistsAfter = activityReminderRepository.findById(type2ReminderId).isPresent();
        assertFalse(reminderExistsAfter, "type=2的提醒应该被删除");

        // 3. 验证活动记录内容
        var newRecord = activityRecordRepository.findAll().stream()
                .filter(record -> record.getActivity().getActivityId().equals(existingActivityId))
                .filter(record -> record.getPet().getPetId().equals(existingPetId))
                .findFirst()
                .orElse(null);

        assertNotNull(newRecord, "应该找到新创建的活动记录");
        assertNull(newRecord.getActivityDescription(), "描述应该为null");
    }

    @Test
    void confirmReminderWithoutDescription_Type1_Success() {
        // 准备 - 记录确认前的状态
        long initialRecordCount = activityRecordRepository.count();
        boolean reminderExistsBefore = activityReminderRepository.findById(type1ReminderId).isPresent();

        // 执行
        reminderService.confirmReminderWithoutDescription(type1ReminderId);

        // 验证
        // 1. 活动记录应该被创建
        long finalRecordCount = activityRecordRepository.count();
        assertEquals(initialRecordCount + 1, finalRecordCount, "应该创建一个活动记录");

        // 2. type=1的提醒应该保留
        boolean reminderExistsAfter = activityReminderRepository.findById(type1ReminderId).isPresent();
        assertTrue(reminderExistsAfter, "type=1的提醒应该保留");
    }

    @Test
    void confirmReminderWithDescription_Type2_Success() {
        // 准备
        String description = "测试描述内容";
        long initialRecordCount = activityRecordRepository.count();

        // 执行
        reminderService.confirmReminderWithDescription(type2ReminderId, description);

        // 验证
        // 1. 活动记录应该被创建
        long finalRecordCount = activityRecordRepository.count();
        assertEquals(initialRecordCount + 1, finalRecordCount, "应该创建一个活动记录");

        // 2. type=2的提醒应该被删除
        boolean reminderExistsAfter = activityReminderRepository.findById(type2ReminderId).isPresent();
        assertFalse(reminderExistsAfter, "type=2的提醒应该被删除");

        // 3. 验证活动记录内容
        var newRecord = activityRecordRepository.findAll().stream()
                .filter(record -> record.getActivity().getActivityId().equals(existingActivityId))
                .filter(record -> record.getPet().getPetId().equals(existingPetId))
                .findFirst()
                .orElse(null);

        assertNotNull(newRecord, "应该找到新创建的活动记录");
        assertEquals(description, newRecord.getActivityDescription(), "描述应该匹配");
    }

    @Test
    void confirmReminderWithDescription_Type1_Success() {
        // 准备
        String description = "测试描述内容";
        long initialRecordCount = activityRecordRepository.count();

        // 执行
        reminderService.confirmReminderWithDescription(type1ReminderId, description);

        // 验证
        // 1. 活动记录应该被创建
        long finalRecordCount = activityRecordRepository.count();
        assertEquals(initialRecordCount + 1, finalRecordCount, "应该创建一个活动记录");

        // 2. type=1的提醒应该保留
        boolean reminderExistsAfter = activityReminderRepository.findById(type1ReminderId).isPresent();
        assertTrue(reminderExistsAfter, "type=1的提醒应该保留");

        // 3. 验证活动记录内容
        var newRecord = activityRecordRepository.findAll().stream()
                .filter(record -> record.getActivity().getActivityId().equals(existingActivityId))
                .filter(record -> record.getPet().getPetId().equals(existingPetId))
                .findFirst()
                .orElse(null);

        assertNotNull(newRecord, "应该找到新创建的活动记录");
        assertEquals(description, newRecord.getActivityDescription(), "描述应该匹配");
    }

    @Test
    void confirmReminderWithDescription_EmptyDescription() {
        // 准备
        String description = "";
        long initialRecordCount = activityRecordRepository.count();

        // 执行
        reminderService.confirmReminderWithDescription(type2ReminderId, description);

        // 验证
        long finalRecordCount = activityRecordRepository.count();
        assertEquals(initialRecordCount + 1, finalRecordCount, "应该创建一个活动记录");

        // 验证活动记录内容
        var newRecord = activityRecordRepository.findAll().stream()
                .filter(record -> record.getActivity().getActivityId().equals(existingActivityId))
                .filter(record -> record.getPet().getPetId().equals(existingPetId))
                .findFirst()
                .orElse(null);

        assertNotNull(newRecord, "应该找到新创建的活动记录");
        assertEquals(description, newRecord.getActivityDescription(), "描述应该为空字符串");
    }

    @Test
    void confirmReminder_NotFound() {
        // 执行 & 验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reminderService.confirmReminderWithoutDescription(nonExistingReminderId));
        assertTrue(exception.getMessage().contains("提醒记录不存在"));
    }
}