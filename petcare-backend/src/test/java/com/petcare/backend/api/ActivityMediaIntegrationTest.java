package com.petcare.backend.api;// ActivityMediaIntegrationTest.java
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class ActivityMediaIntegrationTest {

    private static final String BASE_URL = "http://localhost:8082"; // 根据实际服务地址修改
    private static final String MEDIA_BASE_URL = "http://localhost:8081"; // 媒体服务地址

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // 测试参数（统一控制）
    private static final Long PET_ID = 393L;
    private static final Long USER_ID = 32L;
    private static final Long ACTIVITY_ID = 419L;
    private static final String DESCRIPTION = "测试活动记录";

    // 测试文件路径
    private static final String TEST_FILE_PATH = "test-image.png"; // 准备一个测试图片文件

    public static void main(String[] args) throws IOException, InterruptedException {

        // 首先验证文件状态
        File testFile = new File(TEST_FILE_PATH);
        System.out.println("=== 文件状态检查 ===");
        System.out.println("文件路径: " + testFile.getAbsolutePath());
        System.out.println("文件是否存在: " + testFile.exists());
        System.out.println("文件大小: " + (testFile.exists() ? testFile.length() + " bytes" : "N/A"));
        System.out.println("=== 检查结束 ===\n");

        System.out.println("=== 开始测试活动记录媒体集成接口 ===\n");

        // 测试1：创建带文件的活动记录
        //Long recordId = testCreateActivityRecordWithFile();

//        if (recordId != null) {
//            // 等待文件上传完成
//            Thread.sleep(2000);
//
//            // 测试2：搜索活动记录（查看媒体文件）
              //testSearchActivityRecords();
//
//            // 测试3：更新活动记录（更换文件）
//            testUpdateActivityRecordWithFile(recordId);
//
//            // 测试4：删除活动记录（验证媒体文件也被删除）
              testDeleteActivityRecord(79L);
//        }

        System.out.println("\n=== 测试完成 ===");
    }

    /**
     * 测试1：创建带文件的活动记录
     */
    private static Long testCreateActivityRecordWithFile() throws IOException, InterruptedException {
        System.out.println("=== 测试1：创建带文件的活动记录 ===");

        // 创建边界分隔符
        String boundary = UUID.randomUUID().toString();

        // 构建multipart/form-data请求体
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String descriptionWithTime = DESCRIPTION + " - " + timestamp;

        // 读取测试文件
        File testFile = new File(TEST_FILE_PATH);
        if (!testFile.exists()) {
            // 如果没有测试文件，创建一个简单的文本文件
            Files.writeString(Path.of("test.txt"), "这是一个测试文件内容");
            testFile = new File("test.txt");
        }

        byte[] fileBytes = Files.readAllBytes(testFile.toPath());
        String fileName = testFile.getName();
        String fileContentType = Files.probeContentType(testFile.toPath());
        if (fileContentType == null) {
            fileContentType = "application/octet-stream";
        }

        System.out.println("=== 调试信息 ===");
        System.out.println("文件名: " + fileName);
        System.out.println("文件大小: " + fileBytes.length + " bytes");
        System.out.println("文件类型: " + fileContentType);
        System.out.println("测试文件路径: " + testFile.getAbsolutePath());
        System.out.println("文件是否存在: " + testFile.exists());

        // 构建multipart请求体
        StringBuilder requestBody = new StringBuilder();

        // 添加文本参数
        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"petId\"\r\n\r\n");
        requestBody.append(PET_ID).append("\r\n");

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"activityId\"\r\n\r\n");
        requestBody.append(ACTIVITY_ID).append("\r\n");

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
        requestBody.append(descriptionWithTime).append("\r\n");

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"userId\"\r\n\r\n");
        requestBody.append(USER_ID).append("\r\n");

        // 添加文件参数 - 这里修复了格式问题
        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        requestBody.append("Content-Type: ").append(fileContentType).append("\r\n\r\n");  // 注意这里有两个\r\n

        // 获取文本部分的字节
        byte[] textPartBytes = requestBody.toString().getBytes();
        byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

        // 构建完整请求体
        byte[] fullRequestBody = new byte[textPartBytes.length + fileBytes.length + endBoundaryBytes.length];
        System.arraycopy(textPartBytes, 0, fullRequestBody, 0, textPartBytes.length);
        System.arraycopy(fileBytes, 0, fullRequestBody, textPartBytes.length, fileBytes.length);
        System.arraycopy(endBoundaryBytes, 0, fullRequestBody, textPartBytes.length + fileBytes.length, endBoundaryBytes.length);

        // 打印请求体大小
        System.out.println("文本部分大小: " + textPartBytes.length + " bytes");
        System.out.println("文件部分大小: " + fileBytes.length + " bytes");
        System.out.println("结束边界大小: " + endBoundaryBytes.length + " bytes");
        System.out.println("总请求体大小: " + fullRequestBody.length + " bytes");
        System.out.println("=== 调试结束 ===\n");

        // 发送请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/activities/records/pet/" + PET_ID))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(fullRequestBody))
                .build();

        System.out.println("请求URL: " + request.uri());
        System.out.println("请求方法: POST");
        System.out.println("Content-Type: multipart/form-data; boundary=" + boundary);

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        System.out.println("响应状态: " + response.statusCode());
        System.out.println("响应内容:");
        System.out.println(formatJson(response.body()));
        System.out.println();

        // 解析响应，获取活动记录ID
        if (response.statusCode() == 200) {
            String responseBody = response.body();
            // 简单解析JSON获取ID（实际应该使用JSON解析库）
            if (responseBody.contains("\"id\"")) {
                String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();
                try {
                    Long recordId = Long.parseLong(idStr);
                    System.out.println("创建的活动记录ID: " + recordId);
                    return recordId;
                } catch (NumberFormatException e) {
                    System.err.println("解析ID失败: " + e.getMessage());
                }
            }
        }

        return null;
    }

    /**
     * 测试2：搜索活动记录
     */
    private static void testSearchActivityRecords() throws IOException, InterruptedException {
        System.out.println("=== 测试2：搜索活动记录（包含媒体文件） ===");

        // 构建查询参数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(7);

        String url = BASE_URL + "/api/activities/records/pet/" + PET_ID;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        System.out.println("请求URL: " + request.uri());
        System.out.println("请求方法: GET");
        System.out.println("响应状态: " + response.statusCode());
        System.out.println("响应内容:");
        System.out.println(formatJson(response.body()));
        System.out.println();
    }

    /**
     * 测试3：更新活动记录（更换文件）
     */
    private static void testUpdateActivityRecordWithFile(Long recordId) throws IOException, InterruptedException {
        System.out.println("=== 测试3：更新活动记录（更换文件） ===");

        // 创建边界分隔符
        String boundary = UUID.randomUUID().toString();

        // 准备更新参数
        String newDescription = DESCRIPTION + " - 已更新 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Long newActivityId = ACTIVITY_ID + 1; // 假设更新为另一个活动类型

        // 读取新的测试文件（或使用不同的文件）
        File newTestFile = new File("test-update.txt");
        Files.writeString(newTestFile.toPath(), "这是更新后的测试文件内容 - " + System.currentTimeMillis());

        byte[] fileBytes = Files.readAllBytes(newTestFile.toPath());
        String fileName = newTestFile.getName();

        // 构建multipart请求体
        StringBuilder requestBody = new StringBuilder();

        // 添加文本参数
        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"newActivityId\"\r\n\r\n");
        requestBody.append(newActivityId).append("\r\n");

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
        requestBody.append(newDescription).append("\r\n");

        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"userId\"\r\n\r\n");
        requestBody.append(USER_ID).append("\r\n");

        // 添加文件参数
        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        requestBody.append("Content-Type: text/plain\r\n\r\n");

        // 构建完整请求体
        byte[] textPartBytes = requestBody.toString().getBytes();
        byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] fullRequestBody = new byte[textPartBytes.length + fileBytes.length + endBoundaryBytes.length];
        System.arraycopy(textPartBytes, 0, fullRequestBody, 0, textPartBytes.length);
        System.arraycopy(fileBytes, 0, fullRequestBody, textPartBytes.length, fileBytes.length);
        System.arraycopy(endBoundaryBytes, 0, fullRequestBody, textPartBytes.length + fileBytes.length, endBoundaryBytes.length);

        // 发送请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/activities/records/" + recordId))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .PUT(BodyPublishers.ofByteArray(fullRequestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        System.out.println("请求URL: " + request.uri());
        System.out.println("请求方法: PUT");
        System.out.println("响应状态: " + response.statusCode());
        System.out.println("响应内容:");
        System.out.println(formatJson(response.body()));
        System.out.println();

        // 清理临时文件
        Files.deleteIfExists(newTestFile.toPath());
    }

    /**
     * 测试4：删除活动记录
     */
    private static void testDeleteActivityRecord(Long recordId) throws IOException, InterruptedException {
        System.out.println("=== 测试4：删除活动记录 ===");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/activities/records/" + recordId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        System.out.println("请求URL: " + request.uri());
        System.out.println("请求方法: DELETE");
        System.out.println("响应状态: " + response.statusCode());
        System.out.println("响应内容:");
        System.out.println(response.body());
        System.out.println();

        // 验证媒体文件是否也被删除
        if (response.statusCode() == 200) {
            System.out.println("验证媒体文件是否已被删除...");
            verifyMediaFilesDeleted(recordId);
        }
    }

    /**
     * 验证媒体文件是否已被删除
     */
    private static void verifyMediaFilesDeleted(Long recordId) throws IOException, InterruptedException {
        // 方法1：调用媒体服务直接查询
        String mediaUrl = MEDIA_BASE_URL + "/api/media/related/ACTIVITY/" + recordId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mediaUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("媒体文件查询结果:");
            System.out.println(formatJson(response.body()));

            // 检查是否返回空数组
            if (response.body().contains("\"data\":[]")) {
                System.out.println("✓ 媒体文件已成功删除");
            } else {
                System.out.println("✗ 媒体文件可能未被删除");
            }
        }
        System.out.println();
    }

    /**
     * 格式化JSON字符串（简单实现）
     */
    private static String formatJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        try {
            // 简单的JSON格式化
            int indentLevel = 0;
            StringBuilder formatted = new StringBuilder();
            boolean inQuotes = false;

            for (char c : json.toCharArray()) {
                if (c == '\"' && (formatted.length() == 0 || formatted.charAt(formatted.length() - 1) != '\\')) {
                    inQuotes = !inQuotes;
                    formatted.append(c);
                } else if (!inQuotes) {
                    if (c == '{' || c == '[') {
                        formatted.append(c).append("\n");
                        indentLevel++;
                        formatted.append("  ".repeat(indentLevel));
                    } else if (c == '}' || c == ']') {
                        formatted.append("\n");
                        indentLevel--;
                        formatted.append("  ".repeat(indentLevel));
                        formatted.append(c);
                    } else if (c == ',') {
                        formatted.append(c).append("\n");
                        formatted.append("  ".repeat(indentLevel));
                    } else if (c == ':') {
                        formatted.append(c).append(" ");
                    } else {
                        formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
            }

            return formatted.toString();
        } catch (Exception e) {
            // 如果格式化失败，返回原始JSON
            return json;
        }
    }

    /**
     * 生成测试图片文件（如果不存在）
     */
    private static void generateTestImage() throws IOException {
        File testFile = new File(TEST_FILE_PATH);
        if (!testFile.exists()) {
            // 创建一个简单的PNG文件（base64编码的最小PNG）
            String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
            byte[] pngBytes = Base64.getDecoder().decode(base64Png);
            Files.write(testFile.toPath(), pngBytes);
            System.out.println("已创建测试图片文件: " + TEST_FILE_PATH);
        }
    }

    static {
        try {
            generateTestImage();
        } catch (IOException e) {
            System.err.println("创建测试文件失败: " + e.getMessage());
        }
    }
}