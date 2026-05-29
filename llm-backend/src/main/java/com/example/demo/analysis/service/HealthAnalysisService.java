package com.example.demo.analysis.service;

import com.example.demo.analysis.dto.AnalysisResult;
import com.example.demo.analysis.engine.VectorStore;
import com.example.demo.analysis.engine.VectorStore.SearchResult;
import com.example.demo.common.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HealthAnalysisService {

    private final RestTemplate loadBalancedRestTemplate;
    private final LlmService llmService;
    private final VectorStore vectorStore;

    public HealthAnalysisService(
            @Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
            LlmService llmService,
            VectorStore vectorStore) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.llmService = llmService;
        this.vectorStore = vectorStore;
    }

    public AnalysisResult analyze(String petId, Integer days, String userRequirement) {
        int analysisDays = (days != null && days > 0) ? days : 30;

        Map<String, Object> petInfo = getPetInfo(petId);
        String petName = (String) petInfo.getOrDefault("name", "宠物");
        String breed = (String) petInfo.getOrDefault("breed", "未知");
        String species = (String) petInfo.getOrDefault("species", "未知");

        List<Map<String, Object>> records = getActivityRecords(petId, analysisDays);
        List<Map<String, Object>> abnormalRecords = getAbnormalRecords(petId);

        List<SearchResult> knowledge = vectorStore.search(breed + " " + species + " 养护", 3);

        String prompt = buildPrompt(petName, breed, species, records, abnormalRecords, knowledge, analysisDays, userRequirement);
        String analysis = llmService.callQwenAI(prompt);

        AnalysisResult result = new AnalysisResult();
        result.setPetId(petId);
        result.setPetName(petName);
        result.setBreed(breed);
        result.setSpecies(species);
        result.setAnalysis(analysis);
        result.setAnalysisType(knowledge.isEmpty() ? "BASIC" : "RAG");
        result.setKnowledgeSources(knowledge.stream().map(k -> {
            AnalysisResult.KnowledgeSource s = new AnalysisResult.KnowledgeSource();
            s.setTitle(k.getChunk().getTitle());
            s.setContent(k.getChunk().getContent());
            s.setScore(k.getScore());
            return s;
        }).collect(Collectors.toList()));

        return result;
    }

    private Map<String, Object> getPetInfo(String petId) {
        String url = "http://petcare-backend/api/pets/" + petId;
        try {
            JsonNode resp = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            if (resp != null && resp.get("success").asBoolean()) {
                JsonNode d = resp.get("data");
                Map<String, Object> info = new HashMap<>();
                info.put("name", d.get("name").asText());
                info.put("breed", d.get("breed").asText());
                info.put("species", d.get("species").asText());
                return info;
            }
        } catch (Exception e) { log.warn("获取宠物信息失败", e); }
        Map<String, Object> f = new HashMap<>();
        f.put("name", "未知"); f.put("breed", "未知"); f.put("species", "未知");
        return f;
    }

    private List<Map<String, Object>> getActivityRecords(String petId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        String url = String.format(
                "http://petcare-backend/api/activities/records/pet/%s?startDate=%sT00:00:00&endDate=%sT23:59:59&page=0&size=100",
                petId, start, end);
        try {
            JsonNode resp = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            if (resp != null && resp.has("content")) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode item : resp.get("content")) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("date", item.get("activityDate").asText());
                    r.put("name", item.get("activityName").asText());
                    r.put("kind", item.has("activityKindName") ? item.get("activityKindName").asText() : "");
                    r.put("description", item.has("activityDescription") ? item.get("activityDescription").asText() : "");
                    list.add(r);
                }
                return list;
            }
        } catch (Exception e) { log.warn("获取活动记录失败", e); }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> getAbnormalRecords(String petId) {
        String url = "http://petcare-backend/api/activities/records/pet/" + petId + "/abnormal";
        try {
            JsonNode resp = loadBalancedRestTemplate.getForObject(url, JsonNode.class);
            if (resp != null && resp.isArray()) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode item : resp) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("date", item.get("activityDate").asText());
                    r.put("description", item.get("activityDescription").asText());
                    r.put("bertResult", item.get("bertResult").asInt());
                    r.put("name", item.get("activityName").asText());
                    list.add(r);
                }
                return list;
            }
        } catch (Exception e) { log.warn("获取异常记录失败", e); }
        return Collections.emptyList();
    }

    private String buildPrompt(String name, String breed, String species,
                                List<Map<String, Object>> records,
                                List<Map<String, Object>> abnormalRecords,
                                List<SearchResult> knowledge,
                                int days, String userReq) {
        StringBuilder p = new StringBuilder();
        p.append("你是一个专业的宠物健康顾问。请根据以下宠物近期的活动记录和健康数据进行分析。\n\n");
        p.append("宠物：").append(name).append("（").append(species).append("，").append(breed).append("）\n\n");

        if (!records.isEmpty()) {
            p.append("过去").append(days).append("天的活动记录：\n");
            for (Map<String, Object> r : records) {
                p.append("- [").append(r.get("date")).append("] ")
                 .append(r.get("name")).append("：").append(r.get("description")).append("\n");
            }
            p.append("\n");
        }

        if (!abnormalRecords.isEmpty()) {
            p.append("异常健康记录：\n");
            for (Map<String, Object> r : abnormalRecords) {
                p.append("- [").append(r.get("date")).append("] ")
                 .append(r.get("name")).append("：").append(r.get("description")).append("\n");
            }
            p.append("\n");
        }

        if (!knowledge.isEmpty()) {
            p.append("该品种的养护知识：\n");
            for (SearchResult k : knowledge) {
                p.append("- 【").append(k.getChunk().getTitle()).append("】")
                 .append(k.getChunk().getContent()).append("\n");
            }
            p.append("\n");
        }

        if (userReq != null && !userReq.trim().isEmpty()) {
            p.append("主人特别关注：").append(userReq).append("\n\n");
        }

        p.append("请从以下几个方面进行分析：\n");
        p.append("1. 整体健康评估\n");
        p.append("2. 活动规律分析（饮食/运动/医疗等是否规律）\n");
        p.append("3. 发现的异常或需要注意的问题\n");
        p.append("4. 具体的改进建议\n\n");
        p.append("请用中文给出详细的、个性化的建议。");
        return p.toString();
    }
}
