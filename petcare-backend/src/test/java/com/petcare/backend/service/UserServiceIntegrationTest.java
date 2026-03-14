package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.exception.PetNotFoundException;
import com.petcare.backend.exception.UserNotFoundException;
import com.petcare.backend.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev") // 直接使用开发环境配置
@Transactional // 每个测试方法在事务中运行，测试完成后回滚
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;
    private Long testPetId;

    @BeforeAll
    void setupOnce() {
        System.out.println("=== 开始集成测试，使用开发环境数据库 ===");
        System.out.println("请确保开发数据库服务已启动，并与 application-dev.yml / program-config.yml 中的配置一致。");

        // 创建测试用户
        User testUser = new User();
        testUser.setName("测试用户");
        testUser.setPasswordHash("test_password_hash");
        User savedUser = userRepository.save(testUser);
        testUserId = savedUser.getUserId();
        System.out.println("创建测试用户，ID: " + testUserId);
    }

    @BeforeEach
    void setUp() {
        // 清理测试数据（可选）
        // cleanUpTestData();
    }

    @AfterEach
    void tearDown() {
        // 测试数据会在事务回滚时自动清理
    }

    @Test
    @DisplayName("创建宠物 - 成功场景")
    void testCreatePet_Success() {
        // 准备测试数据
        CreatePetRequest request = new CreatePetRequest();
        request.setName("集成测试宠物");
        request.setSpecies("猫");
        request.setBreed("测试品种");
        request.setBirthday(LocalDate.of(2021, 6, 1));
        request.setUserId(testUserId);

        // 执行
        Pet createdPet = userService.createPet(request);

        // 验证
        assertNotNull(createdPet, "创建的宠物不应为null");
        assertNotNull(createdPet.getPetId(), "宠物ID应自动生成");
        assertEquals("集成测试宠物", createdPet.getName());
        assertEquals("猫", createdPet.getSpecies());
        assertEquals("测试品种", createdPet.getBreed());
        assertEquals(LocalDate.of(2021, 6, 1), createdPet.getBirthday());
        assertNotNull(createdPet.getCreatedAt(), "创建时间应自动设置");
        assertNotNull(createdPet.getUser(), "用户关联不应为null");
        assertEquals(testUserId, createdPet.getUser().getUserId());

        // 保存测试宠物ID供其他测试使用
        this.testPetId = createdPet.getPetId();

        System.out.println("创建宠物成功，ID: " + createdPet.getPetId());
    }

    @Test
    @DisplayName("创建宠物 - 用户不存在")
    void testCreatePet_UserNotFound() {
        // 准备测试数据
        CreatePetRequest request = new CreatePetRequest();
        request.setName("测试宠物");
        request.setSpecies("狗");
        request.setBreed("测试品种");
        request.setUserId(999999L); // 不存在的用户ID

        // 执行 & 验证
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.createPet(request)
        );

        assertTrue(exception.getMessage().contains("用户不存在"));
        System.out.println("预期的异常: " + exception.getMessage());
    }

    @Test
    @DisplayName("根据ID查询宠物 - 成功场景")
    void testGetPetById_Success() {
        // 先创建测试数据
        CreatePetRequest request = new CreatePetRequest();
        request.setName("查询测试宠物");
        request.setSpecies("兔子");
        request.setBreed("垂耳兔");
        request.setBirthday(LocalDate.of(2022, 1, 10));
        request.setUserId(testUserId);

        Pet createdPet = userService.createPet(request);
        Long petId = createdPet.getPetId();

        // 执行查询
        PetResponse petResponse = userService.getPetById(petId);

        // 验证
        assertNotNull(petResponse);
        assertEquals(petId, petResponse.getPetId());
        assertEquals("查询测试宠物", petResponse.getName());
        assertEquals("兔子", petResponse.getSpecies());
        assertEquals("垂耳兔", petResponse.getBreed());
        assertEquals(LocalDate.of(2022, 1, 10), petResponse.getBirthday());

        // 验证用户信息
        assertNotNull(petResponse.getUserId());
        assertNotNull(petResponse.getUserName());
        assertEquals(testUserId, petResponse.getUserId());
        assertEquals("测试用户", petResponse.getUserName());

        // 验证统计信息
        assertNotNull(petResponse.getStatusRecordCount());
        assertNotNull(petResponse.getActivityRecordCount());

        System.out.println("查询宠物成功，ID: " + petId);
        System.out.println("用户信息 - ID: " + petResponse.getUserId() + ", 名称: " + petResponse.getUserName());
        System.out.println("状态记录数量: " + petResponse.getActivityRecordCount());
        System.out.println("活动记录数量: " + petResponse.getStatusRecordCount());
    }

    @Test
    @DisplayName("根据ID查询宠物 - 不存在的宠物")
    void testGetPetById_NotFound() {
        // 使用一个肯定不存在的ID
        Long nonExistentPetId = 999999L;

        // 执行 & 验证
        PetNotFoundException exception = assertThrows(
                PetNotFoundException.class,
                () -> userService.getPetById(nonExistentPetId)
        );

        assertTrue(exception.getMessage().contains("未找到ID为"));
        System.out.println("预期的异常: " + exception.getMessage());
    }

    @Test
    @DisplayName("查询所有宠物")
    void testGetAllPets() {
        // 先创建一些测试数据
        createTestPets();

        // 执行查询
        List<PetResponse> pets = userService.getAllPets();

        // 验证
        assertNotNull(pets);
        assertFalse(pets.isEmpty(), "宠物列表不应为空");

        // 验证列表中的宠物数据
        pets.forEach(pet -> {
            assertNotNull(pet.getPetId());
            assertNotNull(pet.getName());
            assertNotNull(pet.getSpecies());
            assertNotNull(pet.getCreatedAt());
            assertNotNull(pet.getUserId());
            assertNotNull(pet.getUserName());
        });

        System.out.println("查询到 " + pets.size() + " 只宠物");
        pets.forEach(pet ->
                System.out.println("宠物: " + pet.getName() + " (ID: " + pet.getPetId() + ", 用户: " + pet.getUserName() + ")")
        );
    }

    @Test
    @DisplayName("根据用户ID查询宠物")
    void testGetPetsByUserId() {
        // 先创建一些测试数据
        createTestPets();

        // 执行查询
        List<PetResponse> pets = userService.getPetsByUserId(testUserId);

        // 验证
        assertNotNull(pets);
        assertFalse(pets.isEmpty(), "用户宠物列表不应为空");

        // 验证所有宠物都属于指定用户
        pets.forEach(pet -> {
            assertEquals(testUserId, pet.getUserId());
            assertEquals("测试用户", pet.getUserName());
        });

        System.out.println("用户 " + testUserId + " 有 " + pets.size() + " 只宠物");
        pets.forEach(pet ->
                System.out.println("宠物: " + pet.getName() + " (ID: " + pet.getPetId() + ")")
        );
    }

    @Test
    @DisplayName("获取宠物实体")
    void testGetPetEntityById() {
        // 先创建测试数据
        CreatePetRequest request = new CreatePetRequest();
        request.setName("实体测试宠物");
        request.setSpecies("仓鼠");
        request.setBreed("金丝熊");
        request.setUserId(testUserId);

        Pet createdPet = userService.createPet(request);
        Long petId = createdPet.getPetId();

        // 执行
        Pet petEntity = userService.getPetEntityById(petId);

        // 验证
        assertNotNull(petEntity);
        assertEquals(petId, petEntity.getPetId());
        assertEquals("实体测试宠物", petEntity.getName());
        assertEquals("仓鼠", petEntity.getSpecies());
        assertNotNull(petEntity.getUser());
        assertEquals(testUserId, petEntity.getUser().getUserId());

        System.out.println("获取宠物实体成功: " + petEntity.getName());
    }

    @Test
    @DisplayName("完整流程测试: 创建 -> 查询 -> 验证")
    void testCompleteWorkflow() {
        // 创建宠物
        CreatePetRequest request = new CreatePetRequest();
        request.setName("完整流程测试宠物");
        request.setSpecies("鸟");
        request.setBreed("鹦鹉");
        request.setBirthday(LocalDate.of(2021, 8, 20));
        request.setUserId(testUserId);

        Pet createdPet = userService.createPet(request);
        Long petId = createdPet.getPetId();
        System.out.println("步骤1 - 创建宠物完成, ID: " + petId);

        // 通过Response查询
        PetResponse response = userService.getPetById(petId);
        assertNotNull(response);
        assertEquals("完整流程测试宠物", response.getName());
        assertEquals(testUserId, response.getUserId());
        System.out.println("步骤2 - 通过Response查询完成");

        // 通过Entity查询
        Pet entity = userService.getPetEntityById(petId);
        assertNotNull(entity);
        assertEquals("完整流程测试宠物", entity.getName());
        assertEquals(testUserId, entity.getUser().getUserId());
        System.out.println("步骤3 - 通过Entity查询完成");

        // 验证数据一致性
        assertEquals(createdPet.getName(), response.getName());
        assertEquals(createdPet.getName(), entity.getName());
        System.out.println("步骤4 - 数据一致性验证完成");
    }


    /**
     * 创建多个测试宠物
     */
    private void createTestPets() {
        String[] species = {"猫", "狗", "兔子", "仓鼠"};
        String[] breeds = {"波斯猫", "哈士奇", "垂耳兔", "金丝熊"};

        for (int i = 0; i < species.length; i++) {
            CreatePetRequest request = new CreatePetRequest();
            request.setName("测试宠物_" + (i + 1));
            request.setSpecies(species[i]);
            request.setBreed(breeds[i]);
            request.setBirthday(LocalDate.of(2020 + i, (i % 12) + 1, 1));
            request.setUserId(testUserId);

            userService.createPet(request);
        }
    }
}