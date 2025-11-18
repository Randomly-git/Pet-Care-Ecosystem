package com.petcare.backend.api;

import com.petcare.backend.dto.request.CreateStatusRecordDTO;
import com.petcare.backend.dto.request.UpdateStatusRecordDTO;
import com.petcare.backend.util.ApiTestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Slf4j
public class StatusApiTester {

    private static final Long TEST_USER_ID = 32L;
    private static final Long TEST_PET_ID = 393L;
    private static final Long TEST_STATUS_ID = 133L;

    public static void main(String[] args) {
        log.info("开始测试 StatusController API 接口...\n");

        testGetUserStatuses();
        // testCreateStatus();
        // testUpdateStatusName();
        testGetActiveStatusRecords();
        testGetAllStatusRecordsByPetId();

        // StatusRecord 创建/更新/删除可以按需开启
        // testCreateStatusRecord();
        // testStopStatusRecord(TEST_STATUS_ID);
        // testUpdateStatusRecord(TEST_STATUS_ID);
        // testDeleteStatusRecord(TEST_STATUS_ID);

        // testSoftDeleteStatus();
        // testDeleteStatusAndRecords();

        log.info("StatusController API 接口测试完成！");
    }

    /**
     * 1. 获取用户状态列表（返回 JSON）
     */
    private static void testGetUserStatuses() {
        String url = "http://localhost:8080/api/status/user/" + TEST_USER_ID;
        ResponseEntity<String> response = ApiTestUtil.testGet(url);

        log.info("用户状态 JSON:\n{}\n", response.getBody());
    }

    /**
     * 2. 创建状态（返回 JSON）
     */
    private static void testCreateStatus() {
        String url = "http://localhost:8080/api/status?userId=" + TEST_USER_ID + "&statusName=健康状态";
        ResponseEntity<String> response = ApiTestUtil.testPost(url, null);

        log.info("创建状态 JSON:\n{}\n", response.getBody());
    }

    /**
     * 3. 更新状态名称（返回 JSON）
     */
    private static void testUpdateStatusName() {
        String url = "http://localhost:8080/api/status/" + TEST_STATUS_ID + "/name?newName=更新后的状态";
        ResponseEntity<String> response = ApiTestUtil.testPut(url, null);

        log.info("更新状态名称 JSON:\n{}\n", response.getBody());
    }

    /**
     * 4. 获取活跃状态记录（返回 JSON）
     */
    private static void testGetActiveStatusRecords() {
        String url = "http://localhost:8080/api/status/records/active?petId=" + TEST_PET_ID +
                "&targetDate=" + LocalDate.now();

        ResponseEntity<String> response = ApiTestUtil.testGet(url);
        log.info("活跃状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 4.1 获取宠物所有状态记录（返回 JSON）
     */
    private static void testGetAllStatusRecordsByPetId() {
        String url = "http://localhost:8080/api/status/records/pet/" + TEST_PET_ID;
        ResponseEntity<String> response = ApiTestUtil.testGet(url);

        log.info("宠物全部状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 5. 创建状态记录（返回 JSON）
     */
    private static void testCreateStatusRecord() {
        String url = "http://localhost:8080/api/status/records";

        CreateStatusRecordDTO createDTO = new CreateStatusRecordDTO();
        createDTO.setPetId(TEST_PET_ID);
        createDTO.setStatusId(TEST_STATUS_ID);
        createDTO.setStartDate(LocalDate.now());
        createDTO.setStatusDescription("测试状态记录描述");

        ResponseEntity<String> response = ApiTestUtil.testPost(url, createDTO);
        log.info("创建状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 6. 停止状态记录（返回 JSON）
     */
    private static void testStopStatusRecord(Long recordId) {
        String url = "http://localhost:8080/api/status/records/" + recordId +
                "/stop?endDate=" + LocalDate.now();

        ResponseEntity<String> response = ApiTestUtil.testPut(url, null);
        log.info("停止状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 7. 更新状态记录（返回 JSON）
     */
    private static void testUpdateStatusRecord(Long recordId) {
        String url = "http://localhost:8080/api/status/records";

        UpdateStatusRecordDTO updateDTO = new UpdateStatusRecordDTO();
        updateDTO.setStatusRecordId(recordId);
        updateDTO.setStatusId(TEST_STATUS_ID);
        updateDTO.setStartDate(LocalDate.now().minusDays(1));
        updateDTO.setEndDate(LocalDate.now());
        updateDTO.setStatusDescription("更新后的状态记录描述");

        ResponseEntity<String> response = ApiTestUtil.testPut(url, updateDTO);
        log.info("更新状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 8. 删除状态记录（返回 JSON）
     */
    private static void testDeleteStatusRecord(Long recordId) {
        String url = "http://localhost:8080/api/status/records/" + recordId;
        ResponseEntity<String> response = ApiTestUtil.testDelete(url);

        log.info("删除状态记录 JSON:\n{}\n", response.getBody());
    }

    /**
     * 9. 软删除状态（返回 JSON）
     */
    private static void testSoftDeleteStatus() {
        String url = "http://localhost:8080/api/status/" + TEST_STATUS_ID;
        ResponseEntity<String> response = ApiTestUtil.testDelete(url);

        log.info("软删除状态 JSON:\n{}\n", response.getBody());
    }

    /**
     * 10. 删除状态及其记录（返回 JSON）
     */
    private static void testDeleteStatusAndRecords() {
        String url = "http://localhost:8080/api/status/" + TEST_STATUS_ID + "/with-records";
        ResponseEntity<String> response = ApiTestUtil.testDelete(url);

        log.info("删除状态及其记录 JSON:\n{}\n", response.getBody());
    }
}
