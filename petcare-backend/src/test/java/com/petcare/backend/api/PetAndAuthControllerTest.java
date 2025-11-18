package com.petcare.backend.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class PetAndAuthControllerTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static String authToken = "";
    private static Long testUserId = null;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String registeredUsername = ""; // 新增：保存注册的用户名

    public static void main(String[] args) {
        System.out.println("🚀 === 开始API自动化测试 ===");
        System.out.println("目标服务器: " + BASE_URL);
        System.out.println("=" .repeat(50));

        try {
            // 测试流程
            testRegister();
            testLogin();
            testCreatePet();
            testGetPetsByUserId();

            System.out.println("=" .repeat(50));
            System.out.println("✅ === 所有测试完成 ===");

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testRegister() {
        System.out.println("\n📝 1. 测试用户注册");
        System.out.println("-".repeat(30));

        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> request = new HashMap<>();
        registeredUsername = "testuser_" + System.currentTimeMillis();
        request.put("name", registeredUsername);
        request.put("password", "testpassword123");

        System.out.println("请求数据: " + request);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    BASE_URL + "/auth/register",
                    request,
                    String.class
            );

            System.out.println("📨 响应状态: " + response.getStatusCode());
            System.out.println("📄 响应内容: " + response.getBody());

            // 解析响应获取用户ID
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.has("userId")) {
                    testUserId = data.get("userId").asLong();
                    System.out.println("✅ 注册成功，用户ID: " + testUserId);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 注册测试失败: " + e.getMessage());
        }
    }

    private static void testLogin() {
        System.out.println("\n🔐 2. 测试用户登录");
        System.out.println("-".repeat(30));

        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> request = new HashMap<>();
        // 使用实际注册的用户名，而不是硬编码的"testuser"
        request.put("name", registeredUsername);
        request.put("password", "testpassword123");

        System.out.println("请求数据: " + request);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    BASE_URL + "/auth/login",
                    request,
                    String.class
            );

            System.out.println("📨 响应状态: " + response.getStatusCode());
            System.out.println("📄 响应内容: " + response.getBody());

            // 解析响应获取token
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.has("token")) {
                    authToken = data.get("token").asText();
                    testUserId = data.get("userId").asLong();
                    System.out.println("✅ 登录成功");
                    System.out.println("🔑 Token: " + authToken);
                    System.out.println("👤 用户ID: " + testUserId);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 登录测试失败: " + e.getMessage());
        }
    }


    private static void testCreatePet() {
        System.out.println("\n🐾 5. 测试创建宠物");
        System.out.println("-".repeat(30));

        if (authToken.isEmpty() || testUserId == null) {
            System.out.println("❌ 跳过：未获取到Token或用户ID");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .defaultHeader("Authorization", "Bearer " + authToken)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> request = new HashMap<>();
            request.put("name", "测试宠物_" + System.currentTimeMillis());
            request.put("species", "狗");
            request.put("breed", "金毛寻回犬");
            request.put("birthday", "2022-06-15");
            request.put("userId", testUserId);

            System.out.println("请求数据: " + request);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    BASE_URL + "/pets",
                    request,
                    String.class
            );

            System.out.println("📨 响应状态: " + response.getStatusCode());
            System.out.println("📄 响应内容: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("✅ 宠物创建成功");
            }

        } catch (Exception e) {
            System.err.println("❌ 创建宠物失败: " + e.getMessage());
        }
    }

    private static void testGetPetsByUserId() {
        System.out.println("\n🏠 7. 测试获取用户的宠物列表");
        System.out.println("-".repeat(30));

        if (authToken.isEmpty() || testUserId == null) {
            System.out.println("❌ 跳过：未获取到Token或用户ID");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .defaultHeader("Authorization", "Bearer " + authToken)
                    .build();

            String url = BASE_URL + "/pets/user/" + testUserId;
            System.out.println("请求URL: " + url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            System.out.println("📨 响应状态: " + response.getStatusCode());
            System.out.println("📄 响应内容: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                System.out.println("✅ 用户有 " + data.size() + " 只宠物");
            }

        } catch (Exception e) {
            System.err.println("❌ 获取用户宠物列表失败: " + e.getMessage());
        }
    }
}