package com.petcare.backend.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityApiTest {

    private static final String BASE_URL = "http://localhost:9000/api/activities";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 创建 HttpClient 实例，配置 Keep-Alive
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            // 通过其他方式保持连接，不使用 Connection 头
            .build();

    public static void main(String[] args) throws Exception {
        // 测试数据
        Long testUserId = 1L;
        Long testPetId = 10L;
        Long testActivityId = 1L;
        Long testRecordId = 1L;
        Long testActivityKindId = 1L;

        System.out.println("🚀 开始 Activity API 测试...\n");

        // 1. 测试获取所有活动种类
        //testGetAllActivityKinds();

        // 2. 测试获取用户活动列表（不带种类筛选）
        // testGetActivitiesByUserId(testUserId);

        // 3. 测试获取用户活动列表（带种类筛选）
        //testGetActivitiesByUserIdWithKind(testUserId, testActivityKindId);

        // 4. 测试获取活动详情
        // testGetActivityById(testActivityId);

        // 5. 测试创建新活动
        //testCreateActivity(testUserId, testActivityKindId);

        // 6. 测试更新活动
        // testUpdateActivity();

        // 7. 测试搜索活动记录（无参数）
        // testSearchActivityRecordsNoParams(testPetId);

        // 8. 测试搜索活动记录（带时间范围）
        // testSearchActivityRecordsWithDateRange(testPetId);

        // 9. 测试搜索活动记录（带活动种类）
        // testSearchActivityRecordsWithKind(testPetId, testActivityKindId);

        // 10. 测试创建活动记录
        testCreateActivityRecord(testPetId, testActivityId);

        // 11. 测试更新活动记录
        // testUpdateActivityRecord(testRecordId, testActivityId);

        // 12. 测试删除活动记录
        // testDeleteActivityRecord(testRecordId);

        // 13. 测试软删除活动
        // testDeleteActivity(testActivityId);

        // 14. 测试彻底删除活动
        // testDeleteActivityCompletely(testActivityId);

        System.out.println("✅ 所有 API 测试完成！");
    }

    /**
     * 1. 测试获取所有活动种类
     */
    private static void testGetAllActivityKinds() throws Exception {
        String url = BASE_URL + "/kinds";
        System.out.println("📋 测试获取所有活动种类: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .header("Cache-Control", "no-cache")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 2. 测试获取用户活动列表（不带种类筛选）
     */
    private static void testGetActivitiesByUserId(Long userId) throws Exception {
        String url = BASE_URL + "/user/" + userId;
        System.out.println("👤 测试获取用户活动列表（不带种类筛选）: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 3. 测试获取用户活动列表（带种类筛选）
     */
    private static void testGetActivitiesByUserIdWithKind(Long userId, Long activityKindId) throws Exception {
        String url = BASE_URL + "/user/" + userId + "?activityKindId=" + activityKindId;
        System.out.println("👤 测试获取用户活动列表（带种类筛选）: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 4. 测试获取活动详情
     */
    private static void testGetActivityById(Long activityId) throws Exception {
        String url = BASE_URL + "/" + activityId;
        System.out.println("🔍 测试获取活动详情: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 5. 测试创建新活动
     */
    private static void testCreateActivity(Long userId, Long activityKindId) throws Exception {
        String url = BASE_URL;
        System.out.println("➕ 测试创建新活动: " + url);

        String jsonBody = String.format(
                "{\"activityName\":\"散步活动\",\"activityKindId\":%d,\"userId\":%d}",
                activityKindId, userId
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        sendRequest(request);
    }

    /**
     * 6. 测试更新活动
     */
    private static void testUpdateActivity() throws Exception {
        String url = BASE_URL;
        System.out.println("✏️ 测试更新活动: " + url);

        String jsonBody = "{\"activityId\":1,\"activityName\":\"更新后的散步活动\",\"activityKindId\":2}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        sendRequest(request);
    }

    /**
     * 7. 测试搜索活动记录（无参数）
     */
    private static void testSearchActivityRecordsNoParams(Long petId) throws Exception {
        String url = BASE_URL + "/records/pet/" + petId;
        System.out.println("📊 测试搜索活动记录（无参数）: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 8. 测试搜索活动记录（带时间范围）
     */
    private static void testSearchActivityRecordsWithDateRange(Long petId) throws Exception {
        String startDate = LocalDateTime.now().minusDays(7).format(formatter);
        String endDate = LocalDateTime.now().format(formatter);

        String url = BASE_URL + "/records/pet/" + petId +
                "?startDate=" + startDate +
                "&endDate=" + endDate;

        System.out.println("📊 测试搜索活动记录（带时间范围）: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 9. 测试搜索活动记录（带活动种类）
     */
    private static void testSearchActivityRecordsWithKind(Long petId, Long activityKindId) throws Exception {
        String url = BASE_URL + "/records/pet/" + petId +
                "?activityKindId=" + activityKindId;

        System.out.println("📊 测试搜索活动记录（带活动种类）: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .GET()
                .build();

        sendRequest(request);
    }

    /**
     * 10. 测试创建活动记录
     */
    private static void testCreateActivityRecord(Long petId, Long activityId) throws Exception {
        String description = "今天带宠物散步30分钟";
        String date = LocalDateTime.now().format(formatter);

        // URL 编码处理
        description = java.net.URLEncoder.encode(description, "UTF-8");

        String url = BASE_URL + "/records/pet/" + petId +
                "?activityId=" + activityId +
                "&description=" + description +
                "&date=" + date;

        System.out.println("➕ 测试创建活动记录: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        sendRequest(request);
    }

    /**
     * 11. 测试更新活动记录
     */
    private static void testUpdateActivityRecord(Long recordId, Long newActivityId) throws Exception {
        String description = "更新后的活动描述";
        String date = LocalDateTime.now().format(formatter);

        // URL 编码处理
        description = java.net.URLEncoder.encode(description, "UTF-8");

        String url = BASE_URL + "/records/" + recordId +
                "?newActivityId=" + newActivityId +
                "&description=" + description +
                "&date=" + date;

        System.out.println("✏️ 测试更新活动记录: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        sendRequest(request);
    }

    /**
     * 12. 测试删除活动记录
     */
    private static void testDeleteActivityRecord(Long recordId) throws Exception {
        String url = BASE_URL + "/records/" + recordId;
        System.out.println("🗑️ 测试删除活动记录: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .DELETE()
                .build();

        sendRequest(request);
    }

    /**
     * 13. 测试软删除活动
     */
    private static void testDeleteActivity(Long activityId) throws Exception {
        String url = BASE_URL + "/" + activityId;
        System.out.println("🗑️ 测试软删除活动: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .DELETE()
                .build();

        sendRequest(request);
    }

    /**
     * 14. 测试彻底删除活动
     */
    private static void testDeleteActivityCompletely(Long activityId) throws Exception {
        String url = BASE_URL + "/" + activityId + "/complete";
        System.out.println("🗑️ 测试彻底删除活动: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Java-HttpClient-Test")
                .DELETE()
                .build();

        sendRequest(request);
    }

    /**
     * 发送 HTTP 请求并处理响应
     */
    private static void sendRequest(HttpRequest request) throws Exception {
        System.out.println("--- 发送请求 ---");
        System.out.println("方法: " + request.method());
        System.out.println("URL: " + request.uri());
        System.out.println("头信息: " + request.headers().map());

        try {
            long startTime = System.currentTimeMillis();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("\n📥 收到响应:");
            System.out.println("状态码: " + response.statusCode());
            System.out.println("响应时间: " + duration + "ms");
            System.out.println("响应头: " + response.headers().map());

            String body = response.body();
            if (body != null && !body.trim().isEmpty()) {
                System.out.println("响应体: " + formatJson(body));
            } else {
                System.out.println("响应体: <空>");
            }

        } catch (Exception e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== 请求结束 ===\n");

        // 添加短暂延迟，避免过快发送请求
        try {
            Thread.sleep(500); // 增加延迟到 500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 格式化 JSON 输出
     */
    private static String formatJson(String json) {
        try {
            // 简单的 JSON 格式化
            return json.replace("{", "{\n  ")
                    .replace(",", ",\n  ")
                    .replace("}", "\n}");
        } catch (Exception e) {
            return json; // 如果格式化失败，返回原始字符串
        }
    }
}