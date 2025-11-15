package com.petcare.backend.service;

import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.repository.ActivityKindRepository;
import com.petcare.backend.repository.ActivityRecordRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ActivityServiceIntegrationTest2 {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private ActivityKindRepository activityKindRepository;

    @Autowired
    private PetRepository petRepository;

    private Pet testPet;
    private Activity testActivity;
    private ActivityRecord testActivityRecord;
    private ActivityKind existingActivityKind1;
    private ActivityKind existingActivityKind2;

    @BeforeEach
    void setUp() {
        // 获取数据库中已存在的活动种类
        List<ActivityKind> existingKinds = activityKindRepository.findAll();
        System.out.println("数据库中存在的活动种类数量: " + existingKinds.size());

        assertFalse(existingKinds.isEmpty(), "ActivityKind表中应该有数据");

        // 使用前两个已存在的活动种类
        existingActivityKind1 = existingKinds.get(0);
        if (existingKinds.size() > 1) {
            existingActivityKind2 = existingKinds.get(1);
        } else {
            // 如果只有一个活动种类，使用同一个
            existingActivityKind2 = existingActivityKind1;
        }

        System.out.println("使用的活动种类1: " + existingActivityKind1.getActivityKindName());
        System.out.println("使用的活动种类2: " + existingActivityKind2.getActivityKindName());

        // 获取或创建一个测试宠物
        List<Pet> pets = petRepository.findAll();
        if (pets.isEmpty()) {
            testPet = new Pet();
            testPet.setName("测试宠物");
            testPet.setSpecies("狗");
            testPet = petRepository.save(testPet);
            System.out.println("创建了新宠物: " + testPet.getName());
        } else {
            testPet = pets.get(0);
            System.out.println("使用现有宠物: " + testPet.getName());
        }

        // 创建一个测试活动
        testActivity = new Activity();
        testActivity.setActivityName("散步测试");
        testActivity.setActivityKind(existingActivityKind1);
        testActivity.setPet(testPet);
        testActivity.setState(1);
        testActivity = activityRepository.save(testActivity);
        System.out.println("创建测试活动: " + testActivity.getActivityName());

        // 创建一个测试活动记录
        testActivityRecord = new ActivityRecord();
        testActivityRecord.setActivity(testActivity);
        testActivityRecord.setPet(testPet);
        testActivityRecord.setActivityDescription("下午散步30分钟");
        testActivityRecord.setActivityDate(LocalDateTime.now().minusDays(1));
        testActivityRecord = activityRecordRepository.save(testActivityRecord);
        System.out.println("创建测试活动记录: " + testActivityRecord.getActivityDescription());
    }

    @Test
    void searchActivityRecords_ShouldReturnRecordsWithDetails() {
        System.out.println("=== 测试 searchActivityRecords（包含详情） ===");

        // 执行查询
        List<ActivityRecordDTO> records = activityService.searchActivityRecords(
                testPet.getPetId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                testActivity.getActivityKind().getActivityKindId()
        );

        System.out.println("查询到的记录数量: " + records.size());
        for (ActivityRecordDTO record : records) {
            System.out.println("记录ID: " + record.getActivityRecordId() +
                    ", 活动名称: " + record.getActivityName() +
                    ", 活动种类: " + record.getActivityKindName() +
                    ", 描述: " + record.getActivityDescription() +
                    ", 日期: " + record.getActivityDate());
        }

        // 验证结果
        assertFalse(records.isEmpty());
        ActivityRecordDTO firstRecord = records.get(0);
        assertEquals(testActivityRecord.getActivityRecordId(), firstRecord.getActivityRecordId());
        assertEquals(testActivity.getActivityName(), firstRecord.getActivityName());
        assertEquals(testActivity.getActivityKind().getActivityKindName(), firstRecord.getActivityKindName());

        System.out.println("=== searchActivityRecords（包含详情）测试完成 ===");
    }

    @Test
    void searchActivityRecords_WithDateRangeOnly_ShouldReturnRecordsWithDetails() {
        System.out.println("=== 测试 searchActivityRecords（仅日期范围，包含详情） ===");

        // 执行查询（不指定活动种类）
        List<ActivityRecordDTO> records = activityService.searchActivityRecords(
                testPet.getPetId(),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                null
        );

        System.out.println("查询到的记录数量: " + records.size());
        for (ActivityRecordDTO record : records) {
            System.out.println("记录ID: " + record.getActivityRecordId() +
                    ", 活动名称: " + record.getActivityName() +
                    ", 活动种类: " + record.getActivityKindName() +
                    ", 描述: " + record.getActivityDescription());
        }

        // 验证结果
        assertFalse(records.isEmpty());

        // 验证返回的 DTO 包含正确的信息
        ActivityRecordDTO firstRecord = records.get(0);
        assertNotNull(firstRecord.getActivityName());
        assertNotNull(firstRecord.getActivityKindName());

        System.out.println("=== searchActivityRecords（仅日期范围，包含详情）测试完成 ===");
    }

    @Test
    void searchActivityRecords_WithAllNullParams_ShouldReturnAllRecords() {
        System.out.println("=== 测试 searchActivityRecords（所有参数为空） ===");

        // 执行查询（所有参数都为空）
        List<ActivityRecordDTO> records = activityService.searchActivityRecords(
                testPet.getPetId(),
                null,  // startDate 为空
                null,  // endDate 为空
                null   // activityKindId 为空
        );

        System.out.println("查询到的记录数量: " + records.size());
        for (ActivityRecordDTO record : records) {
            System.out.println("记录ID: " + record.getActivityRecordId() +
                    ", 活动名称: " + record.getActivityName() +
                    ", 活动种类: " + record.getActivityKindName() +
                    ", 日期: " + record.getActivityDate());
        }

        // 验证结果 - 应该返回该宠物的所有记录
        assertFalse(records.isEmpty());
        System.out.println("=== searchActivityRecords（所有参数为空）测试完成 ===");
    }


    @Test
    void deleteActivityRecord_ShouldRemoveRecord() {
        System.out.println("=== 测试 deleteActivityRecord ===");
        System.out.println("删除前记录ID: " + testActivityRecord.getActivityRecordId());

        // 验证记录存在
        assertTrue(activityRecordRepository.findById(testActivityRecord.getActivityRecordId()).isPresent());
        System.out.println("确认记录存在");

        // 执行删除
        activityService.deleteActivityRecord(testActivityRecord.getActivityRecordId());
        System.out.println("执行删除操作");

        // 验证记录已被删除
        assertFalse(activityRecordRepository.findById(testActivityRecord.getActivityRecordId()).isPresent());
        System.out.println("确认记录已被删除");

        System.out.println("=== deleteActivityRecord 测试完成 ===");
    }

    @Test
    void createActivityRecord_ShouldCreateNewRecord() {
        System.out.println("=== 测试 createActivityRecord ===");

        // 准备数据
        String description = "新创建的活动记录";
        LocalDateTime date = LocalDateTime.now();

        System.out.println("准备创建新记录 - 宠物ID: " + testPet.getPetId() +
                ", 活动ID: " + testActivity.getActivityId());

        // 执行创建
        ActivityRecord newRecord = activityService.createActivityRecord(
                testPet.getPetId(),
                testActivity.getActivityId(),
                description,
                date
        );

        System.out.println("创建的新记录ID: " + newRecord.getActivityRecordId());

        // 验证结果
        assertNotNull(newRecord.getActivityRecordId());
        assertEquals(description, newRecord.getActivityDescription());
        assertEquals(date, newRecord.getActivityDate());
        assertEquals(testPet.getPetId(), newRecord.getPet().getPetId());
        assertEquals(testActivity.getActivityId(), newRecord.getActivity().getActivityId());

        // 验证已保存到数据库
        assertTrue(activityRecordRepository.findById(newRecord.getActivityRecordId()).isPresent());
        System.out.println("确认新记录已保存到数据库");

        System.out.println("=== createActivityRecord 测试完成 ===");
    }

    @Test
    void updateActivityRecord_ShouldUpdateRecord() {
        System.out.println("=== 测试 updateActivityRecord ===");

        // 准备更新数据
        String newDescription = "更新后的活动描述";
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);

        System.out.println("更新前记录: " + testActivityRecord.getActivityDescription());
        System.out.println("更新前日期: " + testActivityRecord.getActivityDate());

        // 执行更新（使用同一个活动ID）
        ActivityRecord updatedRecord = activityService.updateActivityRecord(
                testActivityRecord.getActivityRecordId(),
                testActivity.getActivityId(), // 使用同一个活动
                newDescription,
                newDate
        );

        System.out.println("更新后记录: " + updatedRecord.getActivityDescription());
        System.out.println("更新后日期: " + updatedRecord.getActivityDate());

        // 验证结果
        assertEquals(newDescription, updatedRecord.getActivityDescription());
        assertEquals(newDate, updatedRecord.getActivityDate());
        assertEquals(testActivity.getActivityId(), updatedRecord.getActivity().getActivityId());

        System.out.println("=== updateActivityRecord 测试完成 ===");
    }

    @Test
    void updateActivityRecord_WithDifferentActivity_ShouldUpdateRecord() {
        System.out.println("=== 测试 updateActivityRecord（不同活动） ===");

        // 创建另一个测试活动
        Activity newActivity = new Activity();
        newActivity.setActivityName("跑步测试");
        newActivity.setActivityKind(existingActivityKind2);
        newActivity.setPet(testPet);
        newActivity.setState(1);
        newActivity = activityRepository.save(newActivity);
        System.out.println("创建新活动用于更新: " + newActivity.getActivityName());

        // 准备更新数据
        String newDescription = "更新后的活动描述（不同活动）";
        LocalDateTime newDate = LocalDateTime.now().plusDays(2);

        System.out.println("更新前活动ID: " + testActivityRecord.getActivity().getActivityId());
        System.out.println("目标活动ID: " + newActivity.getActivityId());

        // 执行更新
        ActivityRecord updatedRecord = activityService.updateActivityRecord(
                testActivityRecord.getActivityRecordId(),
                newActivity.getActivityId(),
                newDescription,
                newDate
        );

        System.out.println("更新后活动ID: " + updatedRecord.getActivity().getActivityId());
        System.out.println("更新后描述: " + updatedRecord.getActivityDescription());

        // 验证结果
        assertEquals(newDescription, updatedRecord.getActivityDescription());
        assertEquals(newDate, updatedRecord.getActivityDate());
        assertEquals(newActivity.getActivityId(), updatedRecord.getActivity().getActivityId());

        System.out.println("=== updateActivityRecord（不同活动）测试完成 ===");
    }

    @Test
    void deleteActivityCompletely_ShouldRemoveActivityAndRecords() {
        System.out.println("=== 测试 deleteActivityCompletely ===");

        // 先验证活动记录存在
        assertTrue(activityRecordRepository.findById(testActivityRecord.getActivityRecordId()).isPresent());
        System.out.println("确认活动记录存在: " + testActivityRecord.getActivityRecordId());

        // 验证活动存在
        assertTrue(activityRepository.findById(testActivity.getActivityId()).isPresent());
        System.out.println("确认活动存在: " + testActivity.getActivityId());

        // 执行完全删除
        activityService.deleteActivityCompletely(testActivity.getActivityId());
        System.out.println("执行完全删除操作");

        // 验证活动记录已被删除
        assertFalse(activityRecordRepository.findById(testActivityRecord.getActivityRecordId()).isPresent());
        System.out.println("确认活动记录已被删除");

        // 验证活动已被删除
        assertFalse(activityRepository.findById(testActivity.getActivityId()).isPresent());
        System.out.println("确认活动已被删除");

        System.out.println("=== deleteActivityCompletely 测试完成 ===");
    }

    @Test
    void deleteActivityCompletely_WithNoRecords_ShouldRemoveActivityOnly() {
        System.out.println("=== 测试 deleteActivityCompletely（无记录活动） ===");

        // 创建一个没有记录的活动
        Activity activityWithoutRecords = new Activity();
        activityWithoutRecords.setActivityName("无记录活动");
        activityWithoutRecords.setActivityKind(existingActivityKind1);
        activityWithoutRecords.setPet(testPet);
        activityWithoutRecords.setState(1);
        activityWithoutRecords = activityRepository.save(activityWithoutRecords);
        System.out.println("创建无记录活动: " + activityWithoutRecords.getActivityName());

        // 验证活动存在
        assertTrue(activityRepository.findById(activityWithoutRecords.getActivityId()).isPresent());
        System.out.println("确认无记录活动存在");

        // 执行完全删除
        activityService.deleteActivityCompletely(activityWithoutRecords.getActivityId());
        System.out.println("执行完全删除操作");

        // 验证活动已被删除
        assertFalse(activityRepository.findById(activityWithoutRecords.getActivityId()).isPresent());
        System.out.println("确认无记录活动已被删除");

        System.out.println("=== deleteActivityCompletely（无记录活动）测试完成 ===");
    }
}