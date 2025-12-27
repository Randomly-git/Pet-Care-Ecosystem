package com.example.demo.resolver;

import com.example.demo.dto.PetHealthData;
import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class PetHealthResolver implements GraphQLQueryResolver {

    // 1. 定义两个 RestTemplate
    private final RestTemplate loadBalancedRestTemplate; // 用于调用内部服务
    private final RestTemplate normalRestTemplate;       // 用于调用通义千问 API
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    // 2. 通过构造函数注入
    public PetHealthResolver(
            @Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
            @Qualifier("normalRestTemplate") RestTemplate normalRestTemplate,
            ObjectMapper objectMapper) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.normalRestTemplate = normalRestTemplate;
        this.objectMapper = objectMapper;
    }

    // 只保留Qwen API Key
    private static final String QWEN_API_KEY = "sk-972a298500904e809a415aa9f153caac";

    public PetHealthData petHealthAnalysis(String petId) {
        log.info("GraphQL查询宠物健康分析: petId={}", petId);

        try {
            // 1. 并行获取宠物信息和状态记录（去掉百度搜索）
            CompletableFuture<Map<String, Object>> petInfoFuture = CompletableFuture
                    .supplyAsync(() -> getPetInfo(petId), executor);

            CompletableFuture<List<Map<String, Object>>> statusFuture = CompletableFuture
                    .supplyAsync(() -> getStatusRecords(petId), executor);

            // 2. 等待两个数据源完成后生成AI建议
            CompletableFuture<String> adviceFuture = CompletableFuture
                    .allOf(petInfoFuture, statusFuture)
                    .thenApplyAsync(v -> {
                        Map<String, Object> petInfo = petInfoFuture.join();
                        List<Map<String, Object>> statusRecords = statusFuture.join();

                        // 构建prompt（去掉百度文章部分）
                        String prompt = buildPrompt(petInfo, statusRecords);
                        return callQwenAI(prompt);
                    }, executor);

            // 3. 等待所有结果并构建响应
            Map<String, Object> petInfo = petInfoFuture.join();
            List<Map<String, Object>> statusRecords = statusFuture.join();
            String healthAdvice = adviceFuture.join();

            // 构建响应（去掉articles和relatedLinks）
            PetHealthData response = new PetHealthData();
            response.setPetId(petId);
            response.setName((String) petInfo.get("name"));
            response.setBreed((String) petInfo.get("breed"));
            response.setSpecies((String) petInfo.get("species"));
            response.setHealthAdvice(healthAdvice);

            // 转换状态记录
            List<PetHealthData.StatusRecord> records = new ArrayList<>();
            for (Map<String, Object> record : statusRecords) {
                PetHealthData.StatusRecord status = new PetHealthData.StatusRecord();
                status.setStatusName((String) record.get("statusName"));
                status.setDescription((String) record.get("description"));
                status.setStartDate((String) record.get("startDate"));
                records.add(status);
            }
            response.setStatusRecords(records);

            return response;

        } catch (Exception e) {
            log.error("宠物健康分析失败", e);
            return createErrorResponse(petId, e.getMessage());
        }
    }

    // === 以下是具体的API调用方法 ===

    private Map<String, Object> getPetInfo(String petId) {
        String url = "http://petcare-backend/api/pets/" + petId;
        try {
            JsonNode response = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.get("success").asBoolean()) {
                JsonNode data = response.get("data");
                Map<String, Object> petInfo = new HashMap<>();
                petInfo.put("name", data.get("name").asText());
                petInfo.put("breed", data.get("breed").asText());
                petInfo.put("species", data.get("species").asText());
                return petInfo;
            }
        } catch (Exception e) {
            log.error("获取宠物信息失败", e);
        }
        return Collections.emptyMap();
    }

    private List<Map<String, Object>> getStatusRecords(String petId) {
        String url = "http://petcare-backend/api/status/records/pet/" + petId;
        try {
            JsonNode[] response = loadBalancedRestTemplate.getForObject(url, JsonNode[].class);
            if (response != null) {
                List<Map<String, Object>> records = new ArrayList<>();
                for (JsonNode record : response) {
                    Map<String, Object> status = new HashMap<>();
                    status.put("statusName", record.get("statusName").asText());
                    status.put("description", record.get("statusDescription").asText());
                    status.put("startDate", record.get("startDate").asText());
                    records.add(status);
                }
                return records;
            }
        } catch (Exception e) {
            log.error("获取状态记录失败", e);
        }
        return Collections.emptyList();
    }

    private String callQwenAI(String prompt) {
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        Map<String, Object> request = new HashMap<>();
        request.put("model", "qwen3-max");
        request.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt
        )));

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + QWEN_API_KEY);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(request, headers);

            JsonNode response = normalRestTemplate.postForObject(url, entity, JsonNode.class);

            if (response != null && response.has("choices")) {
                JsonNode choices = response.get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    return choices.get(0).get("message").get("content").asText();
                }
            }
        } catch (Exception e) {
            log.error("Qwen AI调用失败", e);
        }
        return "无法生成健康建议，请稍后重试。";
    }

    private String buildPrompt(Map<String, Object> petInfo,
                               List<Map<String, Object>> statusRecords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下宠物提供健康建议：\n\n");

        prompt.append("宠物基本信息：\n");
        prompt.append("- 名称：").append(petInfo.get("name")).append("\n");
        prompt.append("- 品种：").append(petInfo.get("breed")).append("\n");
        prompt.append("- 物种：").append(petInfo.get("species")).append("\n\n");

        if (!statusRecords.isEmpty()) {
            prompt.append("健康状态记录：\n");
            for (Map<String, Object> record : statusRecords) {
                prompt.append("- ").append(record.get("statusName")).append("：")
                        .append(record.get("description")).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("请提供具体的、可操作的饮食、运动、护理和健康检查建议。");
        return prompt.toString();
    }

    private PetHealthData createErrorResponse(String petId, String error) {
        PetHealthData response = new PetHealthData();
        response.setPetId(petId);
        response.setName("获取失败");
        response.setBreed("未知");
        response.setSpecies("未知");
        response.setHealthAdvice("分析失败：" + error);
        response.setArticles(Collections.emptyList());
        response.setStatusRecords(Collections.emptyList());
        response.setRelatedLinks(Collections.emptyList());
        return response;
    }
}