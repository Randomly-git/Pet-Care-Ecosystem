package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreateFixedActivityDTO;
import com.petcare.backend.dto.request.UpdateFixedActivityDTO;
import com.petcare.backend.dto.response.FixedActivityDTO;
import com.petcare.backend.entity.FixedActivity;
import com.petcare.backend.repository.FixedActivityRepository;
import com.petcare.backend.repository.ActivityReminderRepository;
import com.petcare.backend.service.impl.FixedActivityServiceImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class FixedActivityServiceTest {

    @Autowired
    private FixedActivityServiceImpl fixedActivityService;

    @Autowired
    private FixedActivityRepository fixedActivityRepository;

    @Autowired
    private ActivityReminderRepository activityReminderRepository;

    // 测试参数配置 - 请根据您的实际数据库数据修改这些值
    private final Long EXISTING_PET_ID = 393L;
    private final Long EXISTING_ACTIVITY_ID = 273L;
    private final Long ANOTHER_ACTIVITY_ID = 274L;
    private final Integer INITIAL_GAP_TIME = 7;
    private final Integer UPDATED_GAP_TIME = 14;

    @Test
    void testCreateFixedActivity_Success() {
        // 准备测试数据
        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(ANOTHER_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        // 执行测试
        FixedActivity result = fixedActivityService.createFixedActivity(createDTO);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getFixedActivityId());
        assertEquals(EXISTING_PET_ID, result.getPetId());
        assertEquals(ANOTHER_ACTIVITY_ID, result.getActivityId());
        assertEquals(INITIAL_GAP_TIME, result.getGapTime());

        // 验证提醒记录是否创建
        var reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                ANOTHER_ACTIVITY_ID, 1, EXISTING_PET_ID);
        assertFalse(reminders.isEmpty());
        assertEquals(1, reminders.size());
    }

    @Test
    void testCreateFixedActivity_DuplicateActivity() {
        // 第一次创建
        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(EXISTING_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        // 第一次创建应该成功
        FixedActivity firstResult = fixedActivityService.createFixedActivity(createDTO);
        assertNotNull(firstResult);

        // 第二次创建相同活动应该失败
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fixedActivityService.createFixedActivity(createDTO);
        });

        assertTrue(exception.getMessage().contains("已经存在相同活动的定时设置"));
    }

    @Test
    void testUpdateFixedActivityGapTime_Success() {
        // 先创建一个定时活动
        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(ANOTHER_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        FixedActivity created = fixedActivityService.createFixedActivity(createDTO);
        Long fixedActivityId = created.getFixedActivityId();

        // 准备更新数据
        UpdateFixedActivityDTO updateDTO = new UpdateFixedActivityDTO();
        updateDTO.setFixedActivityId(fixedActivityId);
        updateDTO.setGapTime(UPDATED_GAP_TIME);

        // 执行更新
        FixedActivity updated = fixedActivityService.updateFixedActivityGapTime(updateDTO);

        // 验证结果
        assertNotNull(updated);
        assertEquals(fixedActivityId, updated.getFixedActivityId());
        assertEquals(UPDATED_GAP_TIME, updated.getGapTime());

        // 验证提醒记录是否更新
        var reminders = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                ANOTHER_ACTIVITY_ID, 1, EXISTING_PET_ID);
        assertFalse(reminders.isEmpty());
    }

    @Test
    void testGetFixedActivitiesByPetId_Success() {
        // 先创建一个定时活动
        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(ANOTHER_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);
        FixedActivity created = fixedActivityService.createFixedActivity(createDTO);
        System.out.println("创建的 FixedActivity ID: " + created.getFixedActivityId());

        // 执行查询
        List<FixedActivityDTO> results = fixedActivityService.getFixedActivitiesByPetId(EXISTING_PET_ID);

        // 打印调试信息
        System.out.println("查询结果: " + results);
        System.out.println("结果数量: " + (results != null ? results.size() : "null"));

        if (results != null && !results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                FixedActivityDTO dto = results.get(i);
                System.out.println("第 " + i + " 个结果:");
                System.out.println("  fixedActivityId: " + dto.getFixedActivityId());
                System.out.println("  activityId: " + dto.getActivityId());
                System.out.println("  activityName: " + dto.getActivityName());
                System.out.println("  petId: " + dto.getPetId());
                System.out.println("  petName: " + dto.getPetName());
                System.out.println("  gapTime: " + dto.getGapTime());
                System.out.println("  nextReminderDate: " + dto.getNextReminderDate());
            }
        }

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());

        FixedActivityDTO firstResult = results.get(0);
        assertNotNull(firstResult.getFixedActivityId());
        assertNotNull(firstResult.getActivityName());
        assertNotNull(firstResult.getPetName());
        assertNotNull(firstResult.getNextReminderDate());
    }

    @Test
    void testDeleteFixedActivity_Success() {
        // 先创建一个定时活动
        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(ANOTHER_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        FixedActivity created = fixedActivityService.createFixedActivity(createDTO);
        Long fixedActivityId = created.getFixedActivityId();

        // 验证创建成功
        assertTrue(fixedActivityRepository.existsById(fixedActivityId));

        var remindersBeforeDelete = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                ANOTHER_ACTIVITY_ID, 1, EXISTING_PET_ID);
        assertFalse(remindersBeforeDelete.isEmpty());

        // 执行删除
        fixedActivityService.deleteFixedActivity(fixedActivityId);

        // 验证 FixedActivity 已删除
        assertFalse(fixedActivityRepository.existsById(fixedActivityId));

        // 验证提醒记录已删除
        var remindersAfterDelete = activityReminderRepository.findByActivityIdAndTypeAndPetId(
                ANOTHER_ACTIVITY_ID, 1, EXISTING_PET_ID);
        assertTrue(remindersAfterDelete.isEmpty());
    }

    @Test
    void testCreateFixedActivity_InvalidPetId() {
        Long INVALID_PET_ID = 9999L;

        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(INVALID_PET_ID);
        createDTO.setActivityId(ANOTHER_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            fixedActivityService.createFixedActivity(createDTO);
        });

        assertTrue(exception.getMessage().contains("宠物不存在"));
    }

    @Test
    void testCreateFixedActivity_InvalidActivityId() {
        Long INVALID_ACTIVITY_ID = 9999L;

        CreateFixedActivityDTO createDTO = new CreateFixedActivityDTO();
        createDTO.setPetId(EXISTING_PET_ID);
        createDTO.setActivityId(INVALID_ACTIVITY_ID);
        createDTO.setGapTime(INITIAL_GAP_TIME);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            fixedActivityService.createFixedActivity(createDTO);
        });

        assertTrue(exception.getMessage().contains("活动不存在"));
    }

    @Test
    void testUpdateFixedActivityGapTime_InvalidId() {
        Long INVALID_FIXED_ACTIVITY_ID = 9999L;

        UpdateFixedActivityDTO updateDTO = new UpdateFixedActivityDTO();
        updateDTO.setFixedActivityId(INVALID_FIXED_ACTIVITY_ID);
        updateDTO.setGapTime(UPDATED_GAP_TIME);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            fixedActivityService.updateFixedActivityGapTime(updateDTO);
        });

        assertTrue(exception.getMessage().contains("定时活动不存在"));
    }

    @Test
    void testDeleteFixedActivity_InvalidId() {
        Long INVALID_FIXED_ACTIVITY_ID = 9999L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            fixedActivityService.deleteFixedActivity(INVALID_FIXED_ACTIVITY_ID);
        });

        assertTrue(exception.getMessage().contains("定时活动不存在"));
    }
}