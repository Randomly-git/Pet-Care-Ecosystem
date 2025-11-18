package com.petcare.backend.api;

import com.petcare.backend.dto.request.CreateActivityDTO;
import com.petcare.backend.dto.request.UpdateActivityDTO;
import com.petcare.backend.util.ApiTestUtil;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityApiTest {

    private static final String BASE_URL = "http://localhost:8080/api/activities";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void main(String[] args) {
        // 测试数据
        Long testUserId = 32L;
        Long testPetId = 393L;
        Long testActivityId = 1L;
        Long testRecordId = 1L;
        Long testActivityKindId = 1L;

        System.out.println("🚀 开始 Activity API 测试...\n");

//        // 1. 测试获取所有活动种类
//        testGetAllActivityKinds();
//
//        // 2. 测试获取用户活动列表（不带种类筛选）
//        testGetActivitiesByUserId(testUserId);
//
//        // 3. 测试获取用户活动列表（带种类筛选）
//        testGetActivitiesByUserIdWithKind(testUserId, testActivityKindId);
//
//        // 4. 测试获取活动详情
//        testGetActivityById(testActivityId);
//
//        // 5. 测试创建新活动
          testCreateActivity(testActivityKindId,testUserId);
//
//        // 6. 测试更新活动
//        testUpdateActivity();
//
//        // 7. 测试搜索活动记录（无参数）
//        testSearchActivityRecordsNoParams(testPetId);
//
//        // 8. 测试搜索活动记录（带时间范围）
//        testSearchActivityRecordsWithDateRange(testPetId);
//
//        // 9. 测试搜索活动记录（带活动种类）
//        testSearchActivityRecordsWithKind(testPetId, testActivityKindId);
//
//        // 10. 测试创建活动记录
//        testCreateActivityRecord(testPetId, testActivityId);
//
//        // 11. 测试更新活动记录
//        testUpdateActivityRecord(testRecordId, testActivityId);
//
//        // 12. 测试删除活动记录
//        testDeleteActivityRecord(testRecordId);
//
//        // 13. 测试软删除活动
//        testDeleteActivity(testActivityId);
//
//        // 14. 测试彻底删除活动
//        testDeleteActivityCompletely(testActivityId);

        System.out.println("✅ 所有 API 测试完成！");
    }

    /**
     * 1. 测试获取所有活动种类
     */
    private static void testGetAllActivityKinds() {
        String url = BASE_URL + "/kinds";
        System.out.println("📋 测试获取所有活动种类");
        ApiTestUtil.testGet(url);
    }

    /**
     * 2. 测试获取用户活动列表（不带种类筛选）
     */
    private static void testGetActivitiesByUserId(Long userId) {
        String url = BASE_URL + "/user/" + userId;
        System.out.println("👤 测试获取用户活动列表（不带种类筛选）");
        ApiTestUtil.testGet(url);
    }

    /**
     * 3. 测试获取用户活动列表（带种类筛选）
     */
    private static void testGetActivitiesByUserIdWithKind(Long userId, Long activityKindId) {
        String url = BASE_URL + "/user/" + userId + "?activityKindId=" + activityKindId;
        System.out.println("👤 测试获取用户活动列表（带种类筛选）");
        ApiTestUtil.testGet(url);
    }

    /**
     * 4. 测试获取活动详情
     */
    private static void testGetActivityById(Long activityId) {
        String url = BASE_URL + "/" + activityId;
        System.out.println("🔍 测试获取活动详情");
        ApiTestUtil.testGet(url);
    }

    /**
     * 5. 测试创建新活动
     */
    private static void testCreateActivity(Long userId, Long activityKindId) {
        String url = BASE_URL;
        CreateActivityDTO createDTO = new CreateActivityDTO();
        createDTO.setActivityName("散步活动");
        createDTO.setActivityKindId(userId);
        createDTO.setUserId(activityKindId);

        System.out.println("➕ 测试创建新活动");
        ApiTestUtil.testPost(url, createDTO);
    }

    /**
     * 6. 测试更新活动
     */
    private static void testUpdateActivity() {
        String url = BASE_URL;
        UpdateActivityDTO updateDTO = new UpdateActivityDTO();
        updateDTO.setActivityId(1L);
        updateDTO.setActivityName("更新后的散步活动");
        updateDTO.setActivityKindId(2L);

        System.out.println("✏️ 测试更新活动");
        ApiTestUtil.testPut(url, updateDTO);
    }

    /**
     * 7. 测试搜索活动记录（无参数）
     */
    private static void testSearchActivityRecordsNoParams(Long petId) {
        String url = BASE_URL + "/records/pet/" + petId;
        System.out.println("📊 测试搜索活动记录（无参数）");
        ApiTestUtil.testGet(url);
    }

    /**
     * 8. 测试搜索活动记录（带时间范围）
     */
    private static void testSearchActivityRecordsWithDateRange(Long petId) {
        String startDate = LocalDateTime.now().minusDays(7).format(formatter);
        String endDate = LocalDateTime.now().format(formatter);

        String url = BASE_URL + "/records/pet/" + petId +
                "?startDate=" + startDate +
                "&endDate=" + endDate;

        System.out.println("📊 测试搜索活动记录（带时间范围）");
        ApiTestUtil.testGet(url);
    }

    /**
     * 9. 测试搜索活动记录（带活动种类）
     */
    private static void testSearchActivityRecordsWithKind(Long petId, Long activityKindId) {
        String url = BASE_URL + "/records/pet/" + petId +
                "?activityKindId=" + activityKindId;

        System.out.println("📊 测试搜索活动记录（带活动种类）");
        ApiTestUtil.testGet(url);
    }

    /**
     * 10. 测试创建活动记录
     */
    private static void testCreateActivityRecord(Long petId, Long activityId) {
        String description = "今天带宠物散步30分钟";
        String date = LocalDateTime.now().format(formatter);

        String url = BASE_URL + "/records/pet/" + petId +
                "?activityId=" + activityId +
                "&description=" + description +
                "&date=" + date;

        System.out.println("➕ 测试创建活动记录");

        // 对于 GET 风格的 POST 请求，可以使用空的请求体
        ApiTestUtil.testPost(url, null);
    }

    /**
     * 11. 测试更新活动记录
     */
    private static void testUpdateActivityRecord(Long recordId, Long newActivityId) {
        String description = "更新后的活动描述";
        String date = LocalDateTime.now().format(formatter);

        String url = BASE_URL + "/records/" + recordId +
                "?newActivityId=" + newActivityId +
                "&description=" + description +
                "&date=" + date;

        System.out.println("✏️ 测试更新活动记录");
        ApiTestUtil.testPut(url, null);
    }

    /**
     * 12. 测试删除活动记录
     */
    private static void testDeleteActivityRecord(Long recordId) {
        String url = BASE_URL + "/records/" + recordId;
        System.out.println("🗑️ 测试删除活动记录");
        ApiTestUtil.testDelete(url);
    }

    /**
     * 13. 测试软删除活动
     */
    private static void testDeleteActivity(Long activityId) {
        String url = BASE_URL + "/" + activityId;
        System.out.println("🗑️ 测试软删除活动");
        ApiTestUtil.testDelete(url);
    }

    /**
     * 14. 测试彻底删除活动
     */
    private static void testDeleteActivityCompletely(Long activityId) {
        String url = BASE_URL + "/" + activityId + "/complete";
        System.out.println("🗑️ 测试彻底删除活动");
        ApiTestUtil.testDelete(url);
    }

    /**
     * 辅助方法：打印响应结果
     */
    private static void printResponse(ResponseEntity<String> response) {
        if (response != null && response.getBody() != null) {
            System.out.println("📄 响应 JSON: " + response.getBody());
        } else {
            System.out.println("❌ 无响应内容");
        }
        System.out.println("---");
    }
}