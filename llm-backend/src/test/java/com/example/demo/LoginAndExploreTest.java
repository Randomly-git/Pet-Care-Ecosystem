package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LoginAndExploreTest {

    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate loadBalancedRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USERNAME = "活了一百万次的猫";
    private static final String PASSWORD = "123";

    @Test
    void testLoginAndExplore() {
        System.out.println("\n========== 登录: " + USERNAME + " ==========");

        // 通过网关登录
        String loginUrl = "http://localhost:9000/api/auth/login";
        RestTemplate gatewayRt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginReq = new HttpEntity<>(
                "{\"name\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}", headers);

        try {
            ResponseEntity<JsonNode> loginResp = gatewayRt.postForEntity(loginUrl, loginReq, JsonNode.class);
            System.out.println("登录状态码: " + loginResp.getStatusCode());

            JsonNode body = loginResp.getBody();
            if (body == null || !body.get("success").asBoolean()) {
                System.out.println("❌ 登录失败: " + (body != null ? body.get("message").asText() : "null"));
                return;
            }

            JsonNode data = body.get("data");
            Long userId = data.get("userId").asLong();
            String token = data.get("token").asText();
            String name = data.get("name").asText();
            System.out.println("✅ 登录成功: userId=" + userId + ", name=" + name);

            // 设置 auth header
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.set("Authorization", "Bearer " + token);

            // 查该用户的所有宠物
            String petsUrl = "http://localhost:9000/api/pets/user/" + userId;
            HttpEntity<Void> petsReq = new HttpEntity<>(authHeaders);
            ResponseEntity<JsonNode> petsResp = gatewayRt.exchange(petsUrl, HttpMethod.GET, petsReq, JsonNode.class);

            System.out.println("\n========== 用户" + name + "的宠物 ==========");
            JsonNode petsBody = petsResp.getBody();
            if (petsBody != null && petsBody.get("success").asBoolean()) {
                JsonNode pets = petsBody.get("data");
                System.out.println("宠物数量: " + pets.size());
                for (JsonNode pet : pets) {
                    Long petId = pet.get("petId").asLong();
                    String petName = pet.get("name").asText();
                    String species = pet.get("species").asText();
                    String breed = pet.get("breed").asText();
                    System.out.println("  - petId=" + petId + ", name=" + petName + ", species=" + species + ", breed=" + breed);

                    // 查该宠物的活动记录
                    String recordsUrl = "http://localhost:9000/api/activities/records/pet/" + petId + "?page=0&size=20";
                    HttpEntity<Void> recReq = new HttpEntity<>(authHeaders);
                    ResponseEntity<JsonNode> recResp;
                    try {
                        recResp = gatewayRt.exchange(recordsUrl, HttpMethod.GET, recReq, JsonNode.class);
                        JsonNode recBody = recResp.getBody();
                        if (recBody != null && recBody.has("content")) {
                            JsonNode content = recBody.get("content");
                            System.out.println("    活动记录: " + content.size() + " 条");
                            for (JsonNode rec : content) {
                                System.out.println("      - [" + rec.get("activityDate").asText() + "] "
                                        + rec.get("activityName").asText()
                                        + " | " + rec.get("activityDescription").asText());
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("    获取活动记录失败: " + e.getMessage());
                    }

                    // 查异常记录
                    try {
                        String abUrl = "http://localhost:9000/api/activities/records/pet/" + petId + "/abnormal";
                        ResponseEntity<JsonNode> abResp = gatewayRt.exchange(abUrl, HttpMethod.GET, recReq, JsonNode.class);
                        JsonNode abBody = abResp.getBody();
                        if (abBody != null && abBody.isArray() && abBody.size() > 0) {
                            System.out.println("    异常记录: " + abBody.size() + " 条");
                            for (JsonNode ab : abBody) {
                                System.out.println("      - [" + ab.get("activityDate").asText() + "] "
                                        + ab.get("activityName").asText()
                                        + " | " + ab.get("activityDescription").asText()
                                        + " (BERT=" + ab.get("bertResult").asInt() + ")");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("    获取异常记录失败: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("❌ 获取宠物列表失败: " + (petsBody != null ? petsBody.get("message").asText() : "null"));
            }

        } catch (Exception e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
            fail(e);
        }
    }
}
