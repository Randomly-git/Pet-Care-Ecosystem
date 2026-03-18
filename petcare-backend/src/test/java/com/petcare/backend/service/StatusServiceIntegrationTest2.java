package com.petcare.backend.service;

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