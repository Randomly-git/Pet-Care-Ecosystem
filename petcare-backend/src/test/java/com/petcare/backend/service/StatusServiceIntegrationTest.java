package com.petcare.backend.service;

import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.StatusRecordRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class StatusServiceIntegrationTest {

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private StatusRecordRepository statusRecordRepository;

    private User testUser;
    private Pet testPet;

    @BeforeEach
    void initData() {
        // 创建测试用户
        testUser = new User();
        testUser.setName("测试用户_" + System.currentTimeMillis());
        testUser.setPasswordHash("testpassword");
        testUser = userRepository.save(testUser);

        // 创建测试宠物
        testPet = new Pet();
        testPet.setName("测试宠物_" + System.currentTimeMillis());
        testPet.setSpecies("狗");
        testPet.setUser(testUser);
        testPet = petRepository.save(testPet);

        System.out.println("创建测试用户: ID=" + testUser.getUserId() + ", 宠物: ID=" + testPet.getPetId());

        // 初始化两个状态（关联到宠物）
        Status s1 = new Status();
        s1.setPet(testPet);
        s1.setStatusName("Running");

        Status s2 = new Status();
        s2.setPet(testPet);
        s2.setStatusName("Sleeping");

        statusRepository.saveAll(List.of(s1, s2));

        System.out.println("初始化了2个测试状态");
    }

    /**
     * 1️⃣ 测试：查询有效状态（根据宠物ID）
     */
    @Test
    void testGetValidStatusesByPetId() {
        List<Status> statuses = statusService.getValidStatusesByPetId(testPet.getPetId());
        assertEquals(2, statuses.size());

        System.out.println("✅ 查询有效状态测试通过，找到 " + statuses.size() + " 个状态");
    }

    /**
     * 2️⃣ 测试：新增状态（为宠物创建）
     */
    @Test
    void testCreateStatus() {
        Status created = statusService.createStatus(testPet.getPetId(), "Eating");

        assertNotNull(created.getStatusId());
        assertEquals("Eating", created.getStatusName());
        assertEquals(testPet.getPetId(), created.getPet().getPetId());

        List<Status> all = statusRepository.findByPetPetId(testPet.getPetId());
        assertEquals(3, all.size());

        System.out.println("✅ 新增状态测试通过，创建了状态: " + created.getStatusName());
    }

    /**
     * 3️⃣ 测试：修改状态名称
     */
    @Test
    void testUpdateStatusName() {
        Status status = statusRepository.findAllByPetPetIdAndStatusName(testPet.getPetId(), "Running").get(0);

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
        Status status = statusRepository.findAllByPetPetIdAndStatusName(testPet.getPetId(), "Sleeping").get(0);

        statusService.softDeleteStatus(status.getStatusId());

        // 验证状态已被删除（硬删除）
        assertFalse(statusRepository.findById(status.getStatusId()).isPresent());

        System.out.println("✅ 软删除状态测试通过，状态ID: " + status.getStatusId());
    }

    /**
     * 5️⃣ 测试：重复创建状态应抛异常
     */
    @Test
    void testCreateDuplicateStatus() {
        assertThrows(RuntimeException.class, () ->
                statusService.createStatus(testPet.getPetId(), "Running"));

        System.out.println("✅ 重复创建状态异常测试通过");
    }

    /**
     * 6️⃣ 测试：获取不存在的宠物的状态
     */
    @Test
    void testGetStatusesForNonExistentPet() {
        List<Status> statuses = statusService.getValidStatusesByPetId(999999L);
        assertTrue(statuses.isEmpty());

        System.out.println("✅ 不存在的宠物状态查询测试通过");
    }

    /**
     * 7️⃣ 测试：为不存在的宠物创建状态应抛异常
     */
    @Test
    void testCreateStatusForNonExistentPet() {
        assertThrows(RuntimeException.class, () ->
                statusService.createStatus(999999L, "TestStatus"));

        System.out.println("✅ 为不存在宠物创建状态异常测试通过");
    }

    /**
     * 8️⃣ 测试：创建状态记录
     */
    @Test
    void testCreateStatusRecord() {
        // 获取一个状态
        Status status = statusRepository.findAllByPetPetIdAndStatusName(testPet.getPetId(), "Running").get(0);

        // 创建状态记录DTO
        com.petcare.backend.dto.request.CreateStatusRecordDTO createDTO =
                new com.petcare.backend.dto.request.CreateStatusRecordDTO();
        createDTO.setPetId(testPet.getPetId());
        createDTO.setStatusId(status.getStatusId());
        createDTO.setStartDate(LocalDate.now());
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
        Status status = statusRepository.findAllByPetPetIdAndStatusName(testPet.getPetId(), "Running").get(0);

        // 先创建一些状态记录
        StatusRecord record = new StatusRecord();
        record.setStatus(status);
        record.setPet(testPet);
        record.setStartDate(LocalDate.now());
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
}
