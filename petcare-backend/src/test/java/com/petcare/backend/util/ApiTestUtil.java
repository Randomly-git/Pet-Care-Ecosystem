package com.petcare.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ApiTestUtil {

    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * GET 请求（返回原始 JSON 字符串）
     */
    public static ResponseEntity<String> testGet(String url) {
        log.info("=== 测试 GET 请求: {} ===", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            // 强制要求保持连接，防止 Netty 过快断开
            headers.set(HttpHeaders.CONNECTION, "keep-alive");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            logRawJsonResponse(response);
            return response;
        } catch (Exception e) {
            log.error("GET 请求失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * POST 请求（返回原始 JSON 字符串）
     */
    public static ResponseEntity<String> testPost(String url, Object requestBody) {
        log.info("=== 测试 POST 请求: {} ===", url);
        log.info("请求体(JSON): {}", requestBody);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logRawJsonResponse(response);
            return response;
        } catch (Exception e) {
            log.error("POST 请求失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * PUT 请求（返回原始 JSON 字符串）
     */
    public static ResponseEntity<String> testPut(String url, Object requestBody) {
        log.info("=== 测试 PUT 请求: {} ===", url);
        log.info("请求体(JSON): {}", requestBody);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            logRawJsonResponse(response);
            return response;
        } catch (Exception e) {
            log.error("PUT 请求失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * DELETE 请求（返回原始 JSON 字符串）
     */
    public static ResponseEntity<String> testDelete(String url) {
        log.info("=== 测试 DELETE 请求: {} ===", url);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);

            logRawJsonResponse(response);
            return response;
        } catch (Exception e) {
            log.error("DELETE 请求失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 统一打印 JSON 原始响应
     */
    private static void logRawJsonResponse(ResponseEntity<String> response) {
        log.info("响应状态: {}", response.getStatusCode());

        // 只打印 JSON 字符串，不再打印对象
        String rawJson = response.getBody();
        log.info("响应 JSON: {}", rawJson == null ? "<空>" : rawJson);

        log.info("=== 请求完成 ===\n");
    }
}
