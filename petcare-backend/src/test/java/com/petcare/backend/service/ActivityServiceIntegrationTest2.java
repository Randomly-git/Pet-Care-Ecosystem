package com.petcare.backend.service;

import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.ActivityKindRepository;
import com.petcare.backend.repository.ActivityRecordRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
//@Transactional
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

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Pet testPet;
    private Activity testActivity;
    private ActivityRecord testActivityRecord;
    private ActivityKind existingActivityKind1;
    private ActivityKind existingActivityKind2;
    Long petid;
    Long activitykindid;

    @BeforeEach
    void setUp() {

//        // 清理测试数据
//        activityRepository.deleteAll();
//        activityRecordRepository.deleteAll();
//        petRepository.deleteAll();
//        userRepository.deleteAll();
        petid = 4L;
        activitykindid = 6L;
//
//        // 创建测试用户
//        testUser = new User();
//        testUser.setName("测试用户");
//        testUser.setPasswordHash("testpassword");
//        testUser = userRepository.save(testUser);
//        System.out.println("创建测试用户: " + testUser.getName());
//
//        // 获取数据库中已存在的活动种类
//        List<ActivityKind> existingKinds = activityKindRepository.findAll();
//        System.out.println("数据库中存在的活动种类数量: " + existingKinds.size());
//
//        assertFalse(existingKinds.isEmpty(), "ActivityKind表中应该有数据");
//
//        // 使用前两个已存在的活动种类
//        existingActivityKind1 = existingKinds.get(0);
//        if (existingKinds.size() > 1) {
//            existingActivityKind2 = existingKinds.get(1);
//        } else {
//            // 如果只有一个活动种类，使用同一个
//            existingActivityKind2 = existingActivityKind1;
//        }
//
//        System.out.println("使用的活动种类1: " + existingActivityKind1.getActivityKindName());
//        System.out.println("使用的活动种类2: " + existingActivityKind2.getActivityKindName());
//
//        // 创建测试宠物
//        testPet = new Pet();
//        testPet.setName("测试宠物");
//        testPet.setSpecies("狗");
//        testPet = petRepository.save(testPet);
//        System.out.println("创建测试宠物: " + testPet.getName());
//
//        // 创建一个测试活动（现在关联用户）
//        testActivity = new Activity();
//        testActivity.setActivityName("散步测试");
//        testActivity.setActivityKind(existingActivityKind1);
//        testActivity.setUser(testUser); // 改为关联用户
//        testActivity.setState(1);
//        testActivity = activityRepository.save(testActivity);
//        System.out.println("创建测试活动: " + testActivity.getActivityName());
//
//        // 创建一个测试活动记录（仍然关联宠物）
//        testActivityRecord = new ActivityRecord();
//        testActivityRecord.setActivity(testActivity);
//        testActivityRecord.setPet(testPet);
//        testActivityRecord.setActivityDescription("下午散步30分钟");
//        testActivityRecord.setActivityDate(LocalDateTime.now().minusDays(1));
//        testActivityRecord = activityRecordRepository.save(testActivityRecord);
//        System.out.println("创建测试活动记录: " + testActivityRecord.getActivityDescription());
    }

    @Test
    void searchActivityRecords_ShouldReturnRecordsWithDetails() {
        System.out.println("=== 测试 searchActivityRecords（分页查询） ===");

        // 准备分页参数：第0页，每页10条，按日期倒序
        Pageable pageable = PageRequest.of(0, 10, Sort.by("activityDate").descending());

        // 执行查询
        Page<ActivityRecordDTO> recordPage = activityService.searchActivityRecords(
                petid,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                activitykindid,
                pageable
        );

        System.out.println("查询到的总记录数: " + recordPage.getTotalElements());
        System.out.println("当前页记录数量: " + recordPage.getContent().size());

        // 验证结果
        assertFalse(recordPage.isEmpty(), "查询结果不应为空");
        assertTrue(recordPage.getTotalElements() >= 1);

        ActivityRecordDTO firstRecord = recordPage.getContent().get(0);
        assertEquals(testActivityRecord.getActivityRecordId(), firstRecord.getActivityRecordId());
        assertEquals(testActivity.getActivityName(), firstRecord.getActivityName());

        System.out.println("=== searchActivityRecords 分页测试完成 ===");
    }

    @Test
    void searchActivityRecords_WithDateRangeOnly_ShouldReturnRecordsWithDetails() {
        System.out.println("=== 测试 searchActivityRecords（仅日期范围分页） ===");

        Pageable pageable = PageRequest.of(0, 5);

        // 执行查询（不指定活动种类）
        Page<ActivityRecordDTO> recordPage = activityService.searchActivityRecords(
                petid,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                null,
                pageable
        );

        // 验证分页元数据
        assertNotNull(recordPage);
        assertTrue(recordPage.getContent().size() > 0);

        // 验证返回的 DTO 包含正确的信息
        ActivityRecordDTO firstRecord = recordPage.getContent().get(0);
        assertNotNull(firstRecord.getActivityName());
        assertNotNull(firstRecord.getActivityKindName());

        System.out.println("=== searchActivityRecords 仅日期范围分页测试完成 ===");
    }

    @Test
    void searchActivityRecords_WithAllNullParams_ShouldReturnAllRecords() {
        System.out.println("=== 测试 searchActivityRecords（所有参数为空，分页） ===");

        // 故意设置一个很小的分页来测试分页逻辑
        Pageable pageable = PageRequest.of(0, 1);

        // 执行查询
        Page<ActivityRecordDTO> recordPage = activityService.searchActivityRecords(
                petid,
                null,
                null,
                null,
                pageable
        );

        // 验证结果
        assertFalse(recordPage.isEmpty());
        // 如果你数据库里有多条记录，这里可以验证 size 是否受限于分页大小
        assertEquals(1, recordPage.getContent().size());

        System.out.println("总页数: " + recordPage.getTotalPages());
        System.out.println("=== searchActivityRecords 所有参数为空分页测试完成 ===");
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
        LocalDateTime date = LocalDateTime.of(2025,12,5,9,9,9);

//        System.out.println("准备创建新记录 - 宠物ID: " + testPet.getPetId() +
//                ", 活动ID: " + testActivity.getActivityId());

        // 执行创建
        ActivityRecord newRecord = activityService.createActivityRecord(
                393L,
                274L,
                description,
                date
        );

        System.out.println("创建的新记录ID: " + newRecord.getActivityRecordId());

//        // 验证结果
//        assertNotNull(newRecord.getActivityRecordId());
//        assertEquals(description, newRecord.getActivityDescription());
//        assertEquals(date, newRecord.getActivityDate());
//        assertEquals(testPet.getPetId(), newRecord.getPet().getPetId());
//        assertEquals(testActivity.getActivityId(), newRecord.getActivity().getActivityId());
//
//        // 验证已保存到数据库
//        assertTrue(activityRecordRepository.findById(newRecord.getActivityRecordId()).isPresent());
//        System.out.println("确认新记录已保存到数据库");
//
//        System.out.println("=== createActivityRecord 测试完成 ===");
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

        // 创建另一个测试活动（关联用户）
        Activity newActivity = new Activity();
        newActivity.setActivityName("跑步测试");
        newActivity.setActivityKind(existingActivityKind2);
        newActivity.setUser(testUser); // 改为关联用户
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

        // 创建一个没有记录的活动（关联用户）
        Activity activityWithoutRecords = new Activity();
        activityWithoutRecords.setActivityName("无记录活动");
        activityWithoutRecords.setActivityKind(existingActivityKind1);
        activityWithoutRecords.setUser(testUser); // 改为关联用户
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