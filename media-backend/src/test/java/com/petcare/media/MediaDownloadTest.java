package com.petcare.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 媒体微服务文件下载API测试脚本
 * 目标: 测试 GET http://localhost:8081/api/media/download/{key}
 *
 * 运行要求: JDK 11 及以上
 * 运行方式:
 * 1. 确保 media-backend 正在运行 (端口 8081)
 * 2. 编译: javac MediaDownloadTest.java
 * 3. 运行: java MediaDownloadTest
 */
public class MediaDownloadTest {

    // 媒体微服务的地址和端口 (community-backend 依赖 media-backend)
    private static final String MEDIA_SERVICE_URL = "http://localhost:8081";

    // 📌 【请修改】您要测试的文件在 COS 上的 KEY 路径。
    // 假设您的上传逻辑将文件放在 moments/ 目录下。
    private static final String COS_FILE_KEY = "activity/user_32/activity_178/d84339bd-997a-4697-bfdc-4d3f789bbcee.png"; // 替换为一个真实存在的文件KEY！

    // 文件下载后保存的本地路径
    private static final String OUTPUT_DIR = "./downloads";

    public static void main(String[] args) {
        String downloadUrl = MEDIA_SERVICE_URL + "/api/media/download/" + COS_FILE_KEY;
        System.out.println("--- 媒体文件下载测试开始 ---");
        System.out.println("请求 URL: " + downloadUrl);

        try {
            // 1. 创建 HTTP 客户端
            HttpClient client = HttpClient.newBuilder().build();

            // 2. 构建 GET 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .build();

            // 3. 发送请求并处理响应 (使用 InputStreamBodyHandler 处理二进制流)
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // 4. 检查响应状态码
            if (response.statusCode() == 200) {
                // 状态码 200: 下载成功
                String outputFileName = COS_FILE_KEY.substring(COS_FILE_KEY.lastIndexOf('/') + 1);
                Path outputPath = Paths.get(OUTPUT_DIR, outputFileName);

                // 确保输出目录存在
                Files.createDirectories(outputPath.getParent());

                // 写入文件流
                try (InputStream is = response.body()) {
                    Files.copy(is, outputPath);
                }

                System.out.println("✅ 下载成功！文件已保存到: " + outputPath.toAbsolutePath());

            } else if (response.statusCode() == 404) {
                System.err.println("❌ 下载失败！状态码 404: 文件在云存储中未找到。");
            } else {
                System.err.println("❌ 下载失败！状态码: " + response.statusCode());
                System.err.println("请检查 media-backend 日志 (端口 8081) 获取 500 错误的详细信息。");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("网络或文件操作失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("--- 媒体文件下载测试结束 ---");
        }
    }
}
