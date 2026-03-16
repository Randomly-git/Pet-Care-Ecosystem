package com.petcare.backend.service;

import com.petcare.backend.dto.response.StatusRecordDTO;
import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.entity.Status;
import com.petcare.backend.entity.StatusRecord;
import com.petcare.backend.entity.Pet;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.StatusRecordRepository;
import com.petcare.backend.repository.StatusRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.UserRepository;
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
//@Transactional
class StatusServiceIntegrationTest2 {

    @Autowired
    private StatusService statusService;

    @Autowired
    private StatusRecordRepository statusRecordRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private Pet testPet;
    private Status testStatus;
    private StatusRecord testStatusRecord;

    @BeforeEach
    void setUp() {
        // 获取或创建测试用户
        List<User> users = userRepository.findAll();
        User testUser;
        if (users.isEmpty()) {
            testUser = new User();
            testUser.setName("测试用户");
            testUser.setPasswordHash("testpassword");
            testUser = userRepository.save(testUser);
        } else {
            testUser = users.get(0);
        }

        // 获取或创建测试宠物
        List<Pet> pets = petRepository.findAll();
        if (pets.isEmpty()) {
            testPet = new Pet();
            testPet.setName("测试宠物");
            testPet.setSpecies("狗");
            testPet = petRepository.save(testPet);
        } else {
            testPet = pets.get(0);
        }

        // 创建测试状态（关联到宠物）
        testStatus = new Status();
        testStatus.setStatusName("健康状态");
        testStatus.setPet(testPet); // 关联到宠物
        testStatus = statusRepository.save(testStatus);

        // 创建测试状态记录（仍然关联宠物）
        testStatusRecord = new StatusRecord();
        testStatusRecord.setStatus(testStatus);
        testStatusRecord.setPet(testPet); // 状态记录仍然关联宠物
        testStatusRecord.setStartDate(LocalDate.now().minusDays(5));
        testStatusRecord.setStatusDescription("宠物健康状况良好");
        testStatusRecord = statusRecordRepository.save(testStatusRecord);
    }

    @Test
    void getActiveStatusRecordsByPetIdAndDate_ShouldReturnActiveRecords() {
        System.out.println("=== 测试 getActiveStatusRecordsByPetIdAndDate ===");

        LocalDate targetDate = LocalDate.now().minusDays(3);

        List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(
                testPet.getPetId(), targetDate
        );

        System.out.println("在日期 " + targetDate + " 活跃的记录数量: " + records.size());
        for (StatusRecordDTO record : records) {
            System.out.println("记录ID: " + record.getStatusRecordId() +
                    ", 状态名称: " + record.getStatusName() +
                    ", 开始日期: " + record.getStartDate() +
                    ", 结束日期: " + record.getEndDate() +
                    ", 描述: " + record.getStatusDescription());

            // 验证记录在目标日期是活跃的
            assertTrue(record.getStartDate().isBefore(targetDate) || record.getStartDate().isEqual(targetDate));
            assertTrue(record.getEndDate() == null || record.getEndDate().isAfter(targetDate) || record.getEndDate().isEqual(targetDate));
        }
    }

    @Test
    void getActiveStatusRecordsByPetIdAndDate_ShouldIncludeBothActiveAndInactiveWhenNull() {
        System.out.println("=== 测试 getActiveStatusRecordsByPetIdAndDate（空日期包含所有状态记录） ===");

        // 创建一个已结束的记录
        StatusRecord endedRecord = new StatusRecord();
        endedRecord.setStatus(testStatus);
        endedRecord.setPet(testPet);
        endedRecord.setStartDate(LocalDate.now().minusDays(10));
        endedRecord.setEndDate(LocalDate.now().minusDays(5)); // 5天前结束
        endedRecord.setStatusDescription("已结束的状态记录");
        statusRecordRepository.save(endedRecord);

        // 创建一个活跃的记录
        StatusRecord activeRecord = new StatusRecord();
        activeRecord.setStatus(testStatus);
        activeRecord.setPet(testPet);
        activeRecord.setStartDate(LocalDate.now().minusDays(3));
        activeRecord.setEndDate(null); // 未结束
        activeRecord.setStatusDescription("活跃的状态记录");
        statusRecordRepository.save(activeRecord);

        // 查询所有记录（targetDate = null）
        List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(
                testPet.getPetId(), null
        );

        System.out.println("总记录数量（包含活跃和已结束）: " + records.size());

        // 验证包含所有记录（包括已结束的）
        boolean foundEndedRecord = records.stream()
                .anyMatch(record -> record.getEndDate() != null && record.getEndDate().isBefore(LocalDate.now()));
        boolean foundActiveRecord = records.stream()
                .anyMatch(record -> record.getEndDate() == null);

        assertTrue(foundEndedRecord, "应该包含已结束的记录");
        assertTrue(foundActiveRecord, "应该包含活跃的记录");
    }

    @Test
    void getActiveStatusRecordsByPetIdAndDate_WithNullDate_ShouldReturnAllRecords() {
        System.out.println("=== 测试 getActiveStatusRecordsByPetIdAndDate（空日期返回所有记录） ===");

        List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(
                testPet.getPetId(), null
        );

        System.out.println("所有记录数量: " + records.size());

        // 验证返回了该宠物的所有状态记录
        Long totalCount = statusRecordRepository.countByPetPetId(testPet.getPetId());
        assertEquals(totalCount, records.size());

        for (StatusRecordDTO record : records) {
            System.out.println("记录ID: " + record.getStatusRecordId() +
                    ", 状态名称: " + record.getStatusName() +
                    ", 开始日期: " + record.getStartDate() +
                    ", 结束日期: " + record.getEndDate());
        }
    }

    @Test
    void getActiveStatusRecordsByPetIdAndDate_ShouldNotReturnEndedRecords() {
        System.out.println("=== 测试 getActiveStatusRecordsByPetIdAndDate（不返回已结束记录） ===");

        // 创建一个已结束的记录
        StatusRecord endedRecord = new StatusRecord();
        endedRecord.setStatus(testStatus);
        endedRecord.setPet(testPet);
        endedRecord.setStartDate(LocalDate.now().minusDays(10));
        endedRecord.setEndDate(LocalDate.now().minusDays(5)); // 5天前结束
        endedRecord.setStatusDescription("已结束的状态记录");
        statusRecordRepository.save(endedRecord);

        // 查询今天的状态，应该不包含已结束的记录
        List<StatusRecordDTO> records = statusService.getActiveStatusRecordsByPetIdAndDate(
                testPet.getPetId(), LocalDate.now()
        );

        System.out.println("活跃记录数量: " + records.size());
        for (StatusRecordDTO record : records) {
            System.out.println("记录ID: " + record.getStatusRecordId() +
                    ", 状态名称: " + record.getStatusName());
            // 验证不包含已结束的记录
            assertNotEquals(endedRecord.getStatusRecordId(), record.getStatusRecordId());
        }
    }

    @Test
    void createStatusRecord_ShouldCreateNewRecord() {
        System.out.println("=== 测试 createStatusRecord ===");

        CreateStatusRecordDTO createDTO = new CreateStatusRecordDTO();
        createDTO.setPetId(testPet.getPetId());
        createDTO.setStatusId(testStatus.getStatusId());
        createDTO.setStartDate(LocalDate.now());
        createDTO.setStatusDescription("新创建的状态记录");

        StatusRecord newRecord = statusService.createStatusRecord(createDTO);

        assertNotNull(newRecord.getStatusRecordId());
        assertEquals(testStatus.getStatusId(), newRecord.getStatus().getStatusId());
        assertNull(newRecord.getEndDate()); // end_date 应该为 null
    }

    @Test
    void stopStatusRecord_ShouldSetEndDate() {
        System.out.println("=== 测试 stopStatusRecord ===");

        LocalDate endDate = LocalDate.now();
        StatusRecord stoppedRecord = statusService.stopStatusRecord(testStatusRecord.getStatusRecordId(), endDate);

        assertEquals(endDate, stoppedRecord.getEndDate());
    }

    @Test
    void deleteStatusRecord_ShouldRemoveRecord() {
        System.out.println("=== 测试 deleteStatusRecord ===");

        statusService.deleteStatusRecord(testStatusRecord.getStatusRecordId());

        assertFalse(statusRecordRepository.findById(testStatusRecord.getStatusRecordId()).isPresent());
    }

    @Test
    void deleteStatusAndRecords_ShouldRemoveStatusAndRelatedRecords() {
        System.out.println("=== 测试 deleteStatusAndRecords ===");

        statusService.deleteStatusAndRecords(testStatus.getStatusId());

        // 验证状态记录已被删除
        List<StatusRecord> relatedRecords = statusRecordRepository.findByStatusStatusId(testStatus.getStatusId());
        assertTrue(relatedRecords.isEmpty());

        // 验证状态已被删除
        assertFalse(statusRepository.findById(testStatus.getStatusId()).isPresent());
    }

    @Test
    void updateStatusRecord_ShouldUpdateRecord() {
        System.out.println("=== 测试 updateStatusRecord ===");

        UpdateStatusRecordDTO updateDTO = new UpdateStatusRecordDTO();
        updateDTO.setStatusRecordId(testStatusRecord.getStatusRecordId());
        updateDTO.setStatusDescription("更新后的描述");
        updateDTO.setStartDate(LocalDate.now().minusDays(3));

        StatusRecord updatedRecord = statusService.updateStatusRecord(updateDTO);

        assertEquals("更新后的描述", updatedRecord.getStatusDescription());
        assertEquals(LocalDate.now().minusDays(3), updatedRecord.getStartDate());
    }
}