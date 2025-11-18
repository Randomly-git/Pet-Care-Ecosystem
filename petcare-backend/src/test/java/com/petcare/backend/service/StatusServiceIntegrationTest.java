package com.petcare.backend.service;

import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.StatusRecordRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev") // ✅ 使用 application-dev.yml 配置的数据库
@Transactional // 每个测试方法执行后自动回滚数据库，避免污染数据
class StatusServiceIntegrationTest {

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private StatusRecordRepository statusRecordRepository;

    private User testUser;

    @BeforeEach
    void initData() {
        // 创建新的测试用户，避免使用现有数据
        testUser = new User();
        testUser.setName("测试用户_" + System.currentTimeMillis()); // 使用时间戳确保唯一性
        testUser.setPasswordHash("testpassword");
        testUser = userRepository.save(testUser);

        System.out.println("创建测试用户: ID=" + testUser.getUserId() + ", 名称=" + testUser.getName());

        // 初始化两个状态
        Status s1 = new Status();
        s1.setUser(testUser);
        s1.setStatusName("Running");
        s1.setState(1);

        Status s2 = new Status();
        s2.setUser(testUser);
        s2.setStatusName("Sleeping");
        s2.setState(1);

        statusRepository.saveAll(List.of(s1, s2));

        System.out.println("初始化了2个测试状态");
    }

    /**
     * 1️⃣ 测试：查询有效状态
     */
    @Test
    void testGetValidStatusesByUserId() {
        List<Status> statuses = statusService.getValidStatusesByUserId(testUser.getUserId());
        assertEquals(2, statuses.size());
        assertTrue(statuses.stream().allMatch(s -> s.getState() == 1));

        System.out.println("✅ 查询有效状态测试通过，找到 " + statuses.size() + " 个状态");
    }

    /**
     * 2️⃣ 测试：新增状态
     */
    @Test
    void testCreateStatus() {
        Status created = statusService.createStatus(testUser.getUserId(), "Eating");

        assertNotNull(created.getStatusId());
        assertEquals("Eating", created.getStatusName());
        assertEquals(1, created.getState());
        assertEquals(testUser.getUserId(), created.getUser().getUserId());

        List<Status> all = statusRepository.findByUserUserId(testUser.getUserId());
        assertEquals(3, all.size());

        System.out.println("✅ 新增状态测试通过，创建了状态: " + created.getStatusName());
    }

    /**
     * 3️⃣ 测试：修改状态名称
     */
    @Test
    void testUpdateStatusName() {
        Status status = statusRepository.findByUserUserIdAndStatusName(testUser.getUserId(), "Running").get(0);

        Status updated = statusService.updateStatusName(status.getStatusId(), "Walking");
        assertEquals("Walking", updated.getStatusName());

        Status dbStatus = statusRepository.findById(status.getStatusId()).orElseThrow();
        assertEquals("Walking", dbStatus.getStatusName());

        System.out.println("✅ 修改状态名称测试通过，从 'Running' 改为 'Walking'");
    }

    /**
     * 4️⃣ 测试：软删除状态
     */
    @Test
    void testSoftDeleteStatus() {
        Status status = statusRepository.findByUserUserIdAndStatusName(testUser.getUserId(), "Sleeping").get(0);

        statusService.softDeleteStatus(status.getStatusId());

        Status deleted = statusRepository.findById(status.getStatusId()).orElseThrow();
        assertEquals(0, deleted.getState());

        System.out.println("✅ 软删除状态测试通过，状态ID: " + status.getStatusId());
    }

    /**
     * 5️⃣ 测试：重复创建状态应抛异常
     */
    @Test
    void testCreateDuplicateStatus() {
        assertThrows(RuntimeException.class, () ->
                statusService.createStatus(testUser.getUserId(), "Running"));

        System.out.println("✅ 重复创建状态异常测试通过");
    }

    /**
     * 6️⃣ 测试：获取不存在的用户的状态
     */
    @Test
    void testGetStatusesForNonExistentUser() {
        List<Status> statuses = statusService.getValidStatusesByUserId(999999L); // 使用更大的ID确保不存在
        assertTrue(statuses.isEmpty());

        System.out.println("✅ 不存在的用户状态查询测试通过");
    }

    /**
     * 7️⃣ 测试：为不存在的用户创建状态应抛异常
     */
    @Test
    void testCreateStatusForNonExistentUser() {
        assertThrows(RuntimeException.class, () ->
                statusService.createStatus(999999L, "TestStatus"));

        System.out.println("✅ 为不存在用户创建状态异常测试通过");
    }

    /**
     * 8️⃣ 测试：创建状态记录
     */
    @Test
    void testCreateStatusRecord() {
        // 先创建一个宠物用于测试状态记录
        com.petcare.backend.entity.Pet testPet = new com.petcare.backend.entity.Pet();
        testPet.setName("测试宠物");
        testPet.setSpecies("狗");
        testPet.setUser(testUser);
        testPet = petRepository.save(testPet);

        // 获取一个状态
        Status status = statusRepository.findByUserUserIdAndStatusName(testUser.getUserId(), "Running").get(0);

        // 创建状态记录DTO
        com.petcare.backend.dto.request.CreateStatusRecordDTO createDTO =
                new com.petcare.backend.dto.request.CreateStatusRecordDTO();
        createDTO.setPetId(testPet.getPetId());
        createDTO.setStatusId(status.getStatusId());
        createDTO.setStartDate(java.time.LocalDate.now());
        createDTO.setStatusDescription("测试状态记录");

        // 创建状态记录
        StatusRecord statusRecord = statusService.createStatusRecord(createDTO);

        assertNotNull(statusRecord.getStatusRecordId());
        assertEquals("测试状态记录", statusRecord.getStatusDescription());

        System.out.println("✅ 创建状态记录测试通过，记录ID: " + statusRecord.getStatusRecordId());
    }

    /**
     * 9️⃣ 测试：删除状态及其记录
     */
    @Test
    void testDeleteStatusAndRecords() {
        Status status = statusRepository.findByUserUserIdAndStatusName(testUser.getUserId(), "Running").get(0);

        // 先创建一些状态记录
        com.petcare.backend.entity.Pet testPet = new com.petcare.backend.entity.Pet();
        testPet.setName("测试宠物");
        testPet.setSpecies("狗");
        testPet.setUser(testUser);
        testPet = petRepository.save(testPet);

        StatusRecord record = new StatusRecord();
        record.setStatus(status);
        record.setPet(testPet);
        record.setStartDate(java.time.LocalDate.now());
        record.setStatusDescription("测试记录");
        statusRecordRepository.save(record);

        // 删除状态及其记录
        statusService.deleteStatusAndRecords(status.getStatusId());

        // 验证状态已删除
        assertFalse(statusRepository.findById(status.getStatusId()).isPresent());

        // 验证状态记录已删除
        List<StatusRecord> remainingRecords = statusRecordRepository.findByStatusStatusId(status.getStatusId());
        assertTrue(remainingRecords.isEmpty());

        System.out.println("✅ 删除状态及其记录测试通过");
    }

    // 需要添加 PetRepository
    @Autowired
    private com.petcare.backend.repository.PetRepository petRepository;
}