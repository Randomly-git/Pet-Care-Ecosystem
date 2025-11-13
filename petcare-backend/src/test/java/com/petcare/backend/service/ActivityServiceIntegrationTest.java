// ActivityServiceIntegrationTest.java
package com.petcare.backend.service;

import com.petcare.backend.dto.response.ActivityDTO;
import com.petcare.backend.dto.response.ActivityKindDTO;
import com.petcare.backend.dto.response.CreateActivityDTO;
import com.petcare.backend.dto.response.UpdateActivityDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.repository.ActivityKindRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional  // 注释掉以便在数据库中看到数据
class ActivityServiceIntegrationTest {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityKindRepository activityKindRepository;

    @Autowired
    private PetRepository petRepository;

    private Pet testPet;
    private ActivityKind existingActivityKind1;
    private ActivityKind existingActivityKind2;

    @BeforeEach
    void setUp() {
        System.out.println("\n=== 开始执行 setUp() ===");

        // 清理测试活动数据，但保留ActivityKind表中的已有数据
        System.out.println("清理测试数据...");
        activityRepository.deleteAll();
        petRepository.deleteAll();
        activityRepository.flush(); // 强制刷新

        // 创建测试宠物
        testPet = new Pet();
        testPet.setName("测试宠物");
        testPet.setSpecies("狗");
        testPet.setBreed("金毛");
        testPet.setBirthday(LocalDate.of(2020, 1, 1));
        testPet.setAvatarUrl("http://example.com/avatar.jpg");
        testPet = petRepository.save(testPet);
        System.out.println("创建测试宠物: ID=" + testPet.getPetId() + ", 名称=" + testPet.getName());

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

        System.out.println("使用的活动种类1: ID=" + existingActivityKind1.getActivityKindId() + ", 名称=" + existingActivityKind1.getActivityKindName());
        System.out.println("使用的活动种类2: ID=" + existingActivityKind2.getActivityKindId() + ", 名称=" + existingActivityKind2.getActivityKindName());
        System.out.println("=== setUp() 执行完成 ===\n");
    }

    @Test
    void testGetAllActivityKinds() {
        System.out.println("\n=== 开始执行 testGetAllActivityKinds() ===");

        // 执行
        List<ActivityKindDTO> result = activityService.getAllActivityKinds();
        System.out.println("获取到的活动种类数量: " + result.size());

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty(), "应该返回数据库中已存在的活动种类");

        // 打印所有活动种类详情
        System.out.println("活动种类详情:");
        for (int i = 0; i < result.size(); i++) {
            ActivityKindDTO dto = result.get(i);
            System.out.println("  " + (i + 1) + ". ID=" + dto.getActivityKindId() + ", 名称=" + dto.getActivityKindName());
            assertNotNull(dto.getActivityKindId());
            assertNotNull(dto.getActivityKindName());
            assertFalse(dto.getActivityKindName().trim().isEmpty());
        }

        System.out.println("=== testGetAllActivityKinds() 执行完成 ===\n");
    }

    @Test
    void testCreateActivity() {
        System.out.println("\n=== 开始执行 testCreateActivity() ===");

        // 准备
        CreateActivityDTO createDTO = new CreateActivityDTO();
        createDTO.setActivityName("晨间散步");
        createDTO.setPetId(testPet.getPetId());
        createDTO.setActivityKindId(existingActivityKind1.getActivityKindId());

        System.out.println("创建活动参数: 名称=" + createDTO.getActivityName() +
                ", 宠物ID=" + createDTO.getPetId() +
                ", 活动种类ID=" + createDTO.getActivityKindId());

        // 执行
        Activity result = activityService.createActivity(createDTO);
        activityRepository.flush(); // 强制刷新到数据库

        System.out.println("创建的活动: ID=" + result.getActivityId() +
                ", 名称=" + result.getActivityName() +
                ", 状态=" + result.getState());

        // 验证
        assertNotNull(result);
        assertNotNull(result.getActivityId());
        assertEquals("晨间散步", result.getActivityName());
        assertEquals(testPet.getPetId(), result.getPet().getPetId());
        assertEquals(existingActivityKind1.getActivityKindId(), result.getActivityKind().getActivityKindId());
        assertEquals(1, result.getState()); // 默认状态为有效

        // 验证数据库中的实际数据
        Optional<Activity> dbActivity = activityRepository.findById(result.getActivityId());
        System.out.println("数据库查询结果: " + (dbActivity.isPresent() ? "存在" : "不存在"));
        if (dbActivity.isPresent()) {
            System.out.println("数据库中的活动状态: " + dbActivity.get().getState());
        }

        System.out.println("=== testCreateActivity() 执行完成 ===\n");
    }

    @Test
    void testCreateActivity_WithInvalidPetId_ShouldThrowException() {
        System.out.println("\n=== 开始执行 testCreateActivity_WithInvalidPetId_ShouldThrowException() ===");

        // 准备
        CreateActivityDTO createDTO = new CreateActivityDTO();
        createDTO.setActivityName("测试活动");
        createDTO.setPetId(999L); // 不存在的宠物ID
        createDTO.setActivityKindId(existingActivityKind1.getActivityKindId());

        System.out.println("尝试使用不存在的宠物ID创建活动: " + createDTO.getPetId());

        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> activityService.createActivity(createDTO));
        System.out.println("抛出的异常: " + exception.getMessage());

        System.out.println("=== testCreateActivity_WithInvalidPetId_ShouldThrowException() 执行完成 ===\n");
    }

    @Test
    void testCreateActivity_WithInvalidActivityKindId_ShouldThrowException() {
        System.out.println("\n=== 开始执行 testCreateActivity_WithInvalidActivityKindId_ShouldThrowException() ===");

        // 准备
        CreateActivityDTO createDTO = new CreateActivityDTO();
        createDTO.setActivityName("测试活动");
        createDTO.setPetId(testPet.getPetId());
        createDTO.setActivityKindId(999L); // 不存在的活动种类ID

        System.out.println("尝试使用不存在的活动种类ID创建活动: " + createDTO.getActivityKindId());

        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> activityService.createActivity(createDTO));
        System.out.println("抛出的异常: " + exception.getMessage());

        System.out.println("=== testCreateActivity_WithInvalidActivityKindId_ShouldThrowException() 执行完成 ===\n");
    }

    @Test
    void testGetActivitiesByPetId() {
        System.out.println("\n=== 开始执行 testGetActivitiesByPetId() ===");

        // 准备 - 创建测试活动
        Activity activity1 = createTestActivity("活动1", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        Activity activity2 = createTestActivity("活动2", testPet.getPetId(), existingActivityKind2.getActivityKindId());
        activityRepository.flush();

        System.out.println("创建了2个测试活动: ID=" + activity1.getActivityId() + ", " + activity2.getActivityId());

        // 执行
        List<ActivityDTO> result = activityService.getActivitiesByPetId(testPet.getPetId());
        System.out.println("获取到的活动数量: " + result.size());

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());

        // 打印活动详情
        for (int i = 0; i < result.size(); i++) {
            ActivityDTO dto = result.get(i);
            System.out.println("活动" + (i + 1) + ": ID=" + dto.getActivityId() +
                    ", 名称=" + dto.getActivityName() +
                    ", 种类=" + dto.getActivityKindName());
        }

        System.out.println("=== testGetActivitiesByPetId() 执行完成 ===\n");
    }

    @Test
    void testGetActivitiesByPetId_WithNoActivities() {
        System.out.println("\n=== 开始执行 testGetActivitiesByPetId_WithNoActivities() ===");

        System.out.println("宠物ID: " + testPet.getPetId() + ", 当前没有活动");

        // 执行
        List<ActivityDTO> result = activityService.getActivitiesByPetId(testPet.getPetId());
        System.out.println("获取到的活动数量: " + result.size());

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());

        System.out.println("=== testGetActivitiesByPetId_WithNoActivities() 执行完成 ===\n");
    }

    @Test
    void testGetActivityById() {
        System.out.println("\n=== 开始执行 testGetActivityById() ===");

        // 准备
        Activity savedActivity = createTestActivity("测试活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("创建测试活动: ID=" + savedActivity.getActivityId() + ", 名称=" + savedActivity.getActivityName());

        // 执行
        Optional<ActivityDTO> result = activityService.getActivityById(savedActivity.getActivityId());
        System.out.println("查询结果是否存在: " + result.isPresent());

        // 验证
        assertTrue(result.isPresent());
        ActivityDTO dto = result.get();

        System.out.println("返回的DTO: ID=" + dto.getActivityId() +
                ", 名称=" + dto.getActivityName() +
                ", 种类名称=" + dto.getActivityKindName() +
                ", 宠物名称=" + dto.getPetName() +
                ", 状态=" + dto.getState());

        assertEquals(savedActivity.getActivityId(), dto.getActivityId());
        assertEquals("测试活动", dto.getActivityName());
        assertEquals(existingActivityKind1.getActivityKindName(), dto.getActivityKindName());
        assertEquals(testPet.getName(), dto.getPetName());
        assertEquals(1, dto.getState());

        System.out.println("=== testGetActivityById() 执行完成 ===\n");
    }

    @Test
    void testGetActivitiesByPetId_WithActivityKindFilter() {
        System.out.println("\n=== 开始执行 testGetActivitiesByPetId_WithActivityKindFilter() ===");

        // 准备 - 创建不同种类的测试活动
        Activity activity1 = createTestActivity("散步活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        Activity activity2 = createTestActivity("喂食活动", testPet.getPetId(), existingActivityKind2.getActivityKindId());
        Activity activity3 = createTestActivity("另一个散步", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("创建了3个测试活动:");
        System.out.println("  - " + activity1.getActivityName() + " (种类: " + existingActivityKind1.getActivityKindName() + ")");
        System.out.println("  - " + activity2.getActivityName() + " (种类: " + existingActivityKind2.getActivityKindName() + ")");
        System.out.println("  - " + activity3.getActivityName() + " (种类: " + existingActivityKind1.getActivityKindName() + ")");

        // 测试1: 按宠物ID和活动种类1筛选
        System.out.println("\n测试1: 按宠物ID和活动种类1筛选");
        List<ActivityDTO> result1 = activityService.getActivitiesByPetId(testPet.getPetId(), existingActivityKind1.getActivityKindId());
        System.out.println("筛选结果数量: " + result1.size());

        assertNotNull(result1);
        assertEquals(2, result1.size(), "应该返回2个种类1的活动");

        for (ActivityDTO dto : result1) {
            System.out.println("  - " + dto.getActivityName() + " (种类: " + dto.getActivityKindName() + ")");
            assertEquals(existingActivityKind1.getActivityKindId(), dto.getActivityKindId());
            assertEquals(existingActivityKind1.getActivityKindName(), dto.getActivityKindName());
        }

        // 测试2: 按宠物ID和活动种类2筛选
        System.out.println("\n测试2: 按宠物ID和活动种类2筛选");
        List<ActivityDTO> result2 = activityService.getActivitiesByPetId(testPet.getPetId(), existingActivityKind2.getActivityKindId());
        System.out.println("筛选结果数量: " + result2.size());

        assertNotNull(result2);
        assertEquals(1, result2.size(), "应该返回1个种类2的活动");
        assertEquals("喂食活动", result2.get(0).getActivityName());
        assertEquals(existingActivityKind2.getActivityKindId(), result2.get(0).getActivityKindId());

        // 测试3: 按宠物ID和不存在活动种类筛选
        System.out.println("\n测试3: 按宠物ID和不存在活动种类筛选");
        List<ActivityDTO> result3 = activityService.getActivitiesByPetId(testPet.getPetId(), 999L);
        System.out.println("筛选结果数量: " + result3.size());

        assertNotNull(result3);
        assertTrue(result3.isEmpty(), "应该返回空列表");

        // 测试4: 验证向后兼容性 - 不提供活动种类ID
        System.out.println("\n测试4: 验证向后兼容性 - 不提供活动种类ID");
        List<ActivityDTO> result4 = activityService.getActivitiesByPetId(testPet.getPetId());
        System.out.println("结果数量: " + result4.size());

        assertNotNull(result4);
        assertEquals(3, result4.size(), "应该返回所有3个活动");

        System.out.println("=== testGetActivitiesByPetId_WithActivityKindFilter() 执行完成 ===\n");
    }

    @Test
    void testGetActivityById_WithInvalidId() {
        System.out.println("\n=== 开始执行 testGetActivityById_WithInvalidId() ===");

        System.out.println("尝试查询不存在的活动ID: 999");

        // 执行
        Optional<ActivityDTO> result = activityService.getActivityById(999L);
        System.out.println("查询结果是否存在: " + result.isPresent());

        // 验证
        assertFalse(result.isPresent());

        System.out.println("=== testGetActivityById_WithInvalidId() 执行完成 ===\n");
    }

    @Test
    void testUpdateActivity() {
        System.out.println("\n=== 开始执行 testUpdateActivity() ===");

        // 准备
        Activity savedActivity = createTestActivity("原始活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("原始活动: ID=" + savedActivity.getActivityId() +
                ", 名称=" + savedActivity.getActivityName() +
                ", 种类ID=" + savedActivity.getActivityKind().getActivityKindId());

        UpdateActivityDTO updateDTO = new UpdateActivityDTO();
        updateDTO.setActivityId(savedActivity.getActivityId());
        updateDTO.setActivityName("更新后的活动");
        updateDTO.setActivityKindId(existingActivityKind2.getActivityKindId());

        System.out.println("更新参数: 新名称=" + updateDTO.getActivityName() +
                ", 新种类ID=" + updateDTO.getActivityKindId());

        // 执行
        Activity result = activityService.updateActivity(updateDTO);
        activityRepository.flush();

        System.out.println("更新后的活动: 名称=" + result.getActivityName() +
                ", 种类ID=" + result.getActivityKind().getActivityKindId());

        // 验证
        assertNotNull(result);
        assertEquals("更新后的活动", result.getActivityName());
        assertEquals(existingActivityKind2.getActivityKindId(), result.getActivityKind().getActivityKindId());

        System.out.println("=== testUpdateActivity() 执行完成 ===\n");
    }

    @Test
    void testUpdateActivity_WithPartialUpdate() {
        System.out.println("\n=== 开始执行 testUpdateActivity_WithPartialUpdate() ===");

        // 准备
        Activity savedActivity = createTestActivity("原始活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("原始活动: ID=" + savedActivity.getActivityId() +
                ", 名称=" + savedActivity.getActivityName() +
                ", 种类ID=" + savedActivity.getActivityKind().getActivityKindId());

        UpdateActivityDTO updateDTO = new UpdateActivityDTO();
        updateDTO.setActivityId(savedActivity.getActivityId());
        updateDTO.setActivityName("只更新名称"); // 不更新活动种类

        System.out.println("部分更新: 只更新名称为 '" + updateDTO.getActivityName() + "'");

        // 执行
        Activity result = activityService.updateActivity(updateDTO);
        activityRepository.flush();

        System.out.println("更新后的活动: 名称=" + result.getActivityName() +
                ", 种类ID=" + result.getActivityKind().getActivityKindId());

        // 验证
        assertNotNull(result);
        assertEquals("只更新名称", result.getActivityName());
        assertEquals(existingActivityKind1.getActivityKindId(), result.getActivityKind().getActivityKindId()); // 活动种类保持不变

        System.out.println("=== testUpdateActivity_WithPartialUpdate() 执行完成 ===\n");
    }

    @Test
    void testUpdateActivity_WithInvalidId_ShouldThrowException() {
        System.out.println("\n=== 开始执行 testUpdateActivity_WithInvalidId_ShouldThrowException() ===");

        // 准备
        UpdateActivityDTO updateDTO = new UpdateActivityDTO();
        updateDTO.setActivityId(999L); // 不存在的活动ID
        updateDTO.setActivityName("更新活动");

        System.out.println("尝试更新不存在的活动ID: " + updateDTO.getActivityId());

        // 执行 & 验证
        Exception exception = assertThrows(RuntimeException.class, () -> activityService.updateActivity(updateDTO));
        System.out.println("抛出的异常: " + exception.getMessage());

        System.out.println("=== testUpdateActivity_WithInvalidId_ShouldThrowException() 执行完成 ===\n");
    }

    @Test
    void testDeleteActivity() {
        System.out.println("\n=== 开始执行 testDeleteActivity() ===");

        // 准备
        Activity savedActivity = createTestActivity("待删除活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("创建待删除活动: ID=" + savedActivity.getActivityId() +
                ", 名称=" + savedActivity.getActivityName() +
                ", 初始状态=" + savedActivity.getState());

        // 验证删除前的状态
        Optional<Activity> beforeDelete = activityRepository.findById(savedActivity.getActivityId());
        System.out.println("删除前数据库查询: " + (beforeDelete.isPresent() ? "存在" : "不存在"));
        if (beforeDelete.isPresent()) {
            System.out.println("删除前状态: " + beforeDelete.get().getState());
        }

        // 执行删除
        boolean result = activityService.deleteActivity(savedActivity.getActivityId());
        activityRepository.flush(); // 强制刷新到数据库

        System.out.println("删除操作结果: " + result);

        // 验证删除操作结果
        assertTrue(result, "删除操作应该返回true");

        // 验证活动状态已更新为0（已删除）
        Optional<Activity> deletedActivity = activityRepository.findById(savedActivity.getActivityId());
        System.out.println("删除后数据库查询: " + (deletedActivity.isPresent() ? "存在" : "不存在"));

        if (deletedActivity.isPresent()) {
            Activity activity = deletedActivity.get();
            System.out.println("删除后活动状态: " + activity.getState());
            assertEquals(0, activity.getState(), "活动状态应该为0（已删除）");
        } else {
            System.out.println("警告: 通过findById未找到活动，可能是物理删除了");
        }

        // 验证通过getActivityById无法查询到已删除的活动
        Optional<ActivityDTO> activityDTO = activityService.getActivityById(savedActivity.getActivityId());
        System.out.println("通过getActivityById查询结果: " + (activityDTO.isPresent() ? "存在" : "不存在"));

        if (activityDTO.isPresent()) {
            ActivityDTO dto = activityDTO.get();
            System.out.println("getActivityById返回的DTO状态: " + dto.getState());
        }

        assertFalse(activityDTO.isPresent(), "通过getActivityById不应该查询到已删除的活动");

        System.out.println("=== testDeleteActivity() 执行完成 ===\n");
    }

    @Test
    void testDeleteActivity_WithInvalidId() {
        System.out.println("\n=== 开始执行 testDeleteActivity_WithInvalidId() ===");

        System.out.println("尝试删除不存在的活动ID: 999");

        // 执行
        boolean result = activityService.deleteActivity(999L);
        System.out.println("删除操作结果: " + result);

        // 验证
        assertFalse(result);

        System.out.println("=== testDeleteActivity_WithInvalidId() 执行完成 ===\n");
    }

    @Test
    void testDeleteActivity_AlreadyDeleted() {
        System.out.println("\n=== 开始执行 testDeleteActivity_AlreadyDeleted() ===");

        // 准备
        Activity savedActivity = createTestActivity("已删除活动", testPet.getPetId(), existingActivityKind1.getActivityKindId());
        activityRepository.flush();

        System.out.println("创建活动: ID=" + savedActivity.getActivityId() + ", 名称=" + savedActivity.getActivityName());

        // 第一次删除
        boolean firstDeleteResult = activityService.deleteActivity(savedActivity.getActivityId());
        activityRepository.flush();
        System.out.println("第一次删除结果: " + firstDeleteResult);

        // 验证第一次删除后的状态
        Optional<Activity> afterFirstDelete = activityRepository.findById(savedActivity.getActivityId());
        if (afterFirstDelete.isPresent()) {
            System.out.println("第一次删除后状态: " + afterFirstDelete.get().getState());
        }

        // 执行 - 第二次删除
        boolean secondDeleteResult = activityService.deleteActivity(savedActivity.getActivityId());
        System.out.println("第二次删除结果: " + secondDeleteResult);

        // 验证
        assertFalse(secondDeleteResult, "应该返回false，因为活动已被删除");

        System.out.println("=== testDeleteActivity_AlreadyDeleted() 执行完成 ===\n");
    }

    /**
     * 创建测试活动的辅助方法
     */
    private Activity createTestActivity(String activityName, Long petId, Long activityKindId) {
        CreateActivityDTO createDTO = new CreateActivityDTO();
        createDTO.setActivityName(activityName);
        createDTO.setPetId(petId);
        createDTO.setActivityKindId(activityKindId);

        Activity activity = activityService.createActivity(createDTO);
        System.out.println("创建测试活动: " + activityName + " (ID: " + activity.getActivityId() + ")");
        return activity;
    }
}