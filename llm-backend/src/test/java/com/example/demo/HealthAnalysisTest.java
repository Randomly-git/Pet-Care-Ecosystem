package com.example.demo;

import com.example.demo.analysis.dto.AnalysisResult;
import com.example.demo.analysis.service.HealthAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HealthAnalysisTest {

    @Autowired
    private HealthAnalysisService healthAnalysisService;

    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate loadBalancedRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 使用账号"活了一百万次的猫"的宠物"可乐"（边牧，6条活动记录）
    private static final String PET_ID = "4";

    @Test
    void testPetApi() {
        System.out.println("\n========== 测试1: GET /api/pets/{petId} ==========");
        String url = "http://petcare-backend/api/pets/" + PET_ID;
        try {
            JsonNode resp = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            System.out.println("状态码: 200");
            System.out.println("响应: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resp));
            assertNotNull(resp);
            assertTrue(resp.get("success").asBoolean());
            System.out.println("✅ 宠物信息接口正常");
        } catch (Exception e) {
            System.err.println("❌ 失败: " + e.getMessage());
            fail(e);
        }
    }

    @Test
    void testActivityRecordsApi() {
        System.out.println("\n========== 测试2: GET /api/activities/records/pet/{petId} ==========");
        String url = "http://petcare-backend/api/activities/records/pet/" + PET_ID + "?page=0&size=5";
        try {
            JsonNode resp = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            System.out.println("状态码: 200");
            System.out.println("响应: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resp));
            assertNotNull(resp);
            System.out.println("✅ 活动记录接口正常");
        } catch (Exception e) {
            System.err.println("❌ 失败: " + e.getMessage());
            fail(e);
        }
    }

    @Test
    void testHealthAnalysis() {
        System.out.println("\n========== 测试3: activityHealthAnalysis ==========");
        try {
            AnalysisResult result = healthAnalysisService.analyze(PET_ID, 30, null);
            System.out.println("宠物名: " + result.getPetName());
            System.out.println("品种: " + result.getBreed());
            System.out.println("物种: " + result.getSpecies());
            System.out.println("分析类型: " + result.getAnalysisType());
            System.out.println("知识来源: " + (result.getKnowledgeSources() != null ? result.getKnowledgeSources().size() : 0) + " 条");
            System.out.println("\n--- AI 分析结果 ---");
            System.out.println(result.getAnalysis());
            assertNotNull(result.getAnalysis());
            assertFalse(result.getAnalysis().contains("不可用"));
            System.out.println("\n✅ 健康分析功能正常");
        } catch (Exception e) {
            System.err.println("❌ 失败: " + e.getMessage());
            fail(e);
        }
    }
}
