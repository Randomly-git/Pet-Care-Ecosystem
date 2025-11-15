package com.petcare.backend.service;

import com.petcare.backend.dto.request.CreatePetRequest;
import com.petcare.backend.dto.response.PetResponse;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.exception.PetNotFoundException;
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

    private Long testPetId;

    @BeforeAll
    void setupOnce() {
        System.out.println("=== 开始集成测试，使用开发环境数据库 ===");
        System.out.println("确保开发数据库服务正在运行: 47.100.240.111:3306");
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

        // 保存测试宠物ID供其他测试使用
        this.testPetId = createdPet.getPetId();

        System.out.println("创建宠物成功，ID: " + createdPet.getPetId());
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

        // 验证统计信息
        assertNotNull(petResponse.getStatusCount());
        assertNotNull(petResponse.getActivityCount());
        assertNotNull(petResponse.getStatusRecordCount());
        assertNotNull(petResponse.getActivityRecordCount());

        System.out.println("查询宠物成功，ID: " + petId);
        System.out.println("状态数量: " + petResponse.getStatusCount());
        System.out.println("活动数量: " + petResponse.getActivityCount());
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

        assertEquals("未找到ID为 " + nonExistentPetId + " 的宠物", exception.getMessage());
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
        });

        System.out.println("查询到 " + pets.size() + " 只宠物");
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

        Pet createdPet = userService.createPet(request);
        Long petId = createdPet.getPetId();

        // 执行
        Pet petEntity = userService.getPetEntityById(petId);

        // 验证
        assertNotNull(petEntity);
        assertEquals(petId, petEntity.getPetId());
        assertEquals("实体测试宠物", petEntity.getName());
        assertEquals("仓鼠", petEntity.getSpecies());

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

        Pet createdPet = userService.createPet(request);
        Long petId = createdPet.getPetId();
        System.out.println("步骤1 - 创建宠物完成, ID: " + petId);

        // 通过Response查询
        PetResponse response = userService.getPetById(petId);
        assertNotNull(response);
        assertEquals("完整流程测试宠物", response.getName());
        System.out.println("步骤2 - 通过Response查询完成");

        // 通过Entity查询
        Pet entity = userService.getPetEntityById(petId);
        assertNotNull(entity);
        assertEquals("完整流程测试宠物", entity.getName());
        System.out.println("步骤3 - 通过Entity查询完成");

        // 验证数据一致性
        assertEquals(createdPet.getName(), response.getName());
        assertEquals(createdPet.getName(), entity.getName());
        System.out.println("步骤4 - 数据一致性验证完成");
    }

    @Test
    @DisplayName("边界测试: 空品种")
    void testCreatePet_EmptyBreed() {
        CreatePetRequest request = new CreatePetRequest();
        request.setName("无品种测试宠物");
        request.setSpecies("鱼");
        request.setBreed(null); // 空品种
        request.setBirthday(LocalDate.of(2023, 1, 1));

        Pet createdPet = userService.createPet(request);

        assertNotNull(createdPet);
        assertNull(createdPet.getBreed());
        System.out.println("空品种测试成功，宠物ID: " + createdPet.getPetId());
    }

    @Test
    @DisplayName("边界测试: 长名称")
    void testCreatePet_LongName() {
        CreatePetRequest request = new CreatePetRequest();
        request.setName("这是一个非常长的宠物名称测试看看会不会被截断或者出现问题的情况");
        request.setSpecies("测试物种");
        request.setBreed("测试品种");

        Pet createdPet = userService.createPet(request);

        assertNotNull(createdPet);
        assertEquals("这是一个非常长的宠物名称测试看看会不会被截断或者出现问题的情况", createdPet.getName());
        System.out.println("长名称测试成功，名称长度: " + createdPet.getName().length());
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

            userService.createPet(request);
        }
    }

    /**
     * 清理测试数据（可选）
     */
    /*private void cleanUpTestData() {
        try {
            List<PetResponse> pets = userService.getAllPets();
            pets.stream()
                    .filter(pet -> pet.getName().contains("测试") || pet.getName().contains("集成测试"))
                    .forEach(pet -> {
                        // 注意：实际项目中可能需要先删除关联数据
                        // 这里依赖 @Transactional 回滚，所以不需要手动清理
                    });
        } catch (Exception e) {
            System.err.println("清理测试数据时出错: " + e.getMessage());
        }
    }*/
}