package com.example.demo.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ToolExecutor {

    private final RestTemplate lb;
    private final ObjectMapper om;

    public ToolExecutor(@Qualifier("loadBalancedRestTemplate") RestTemplate lb, ObjectMapper om) {
        this.lb = lb; this.om = om;
    }

    public Result execute(String name, String argsJson, String petId) {
        try {
            Map<String, Object> args = om.readValue(argsJson, Map.class);
            String pid = (String) args.getOrDefault("petId", petId);
            return switch (name) {
                case "get_pet_info" -> getPetInfo(pid);
                case "get_activity_records" -> getRecords(pid, args);
                case "create_activity_record" -> createRecord(pid, args);
                case "get_health_analysis" -> new Result(Map.of("info", "请调用 activityHealthAnalysis GraphQL 接口: {\"petId\":\"%s\"}".formatted(pid)));
                case "search_knowledge" -> searchKnowledge(args);
                default -> new Result(Map.of("error", "未知工具: " + name));
            };
        } catch (Exception e) {
            log.error("工具{}执行失败", name, e);
            return new Result(Map.of("error", e.getMessage()));
        }
    }

    private Result getPetInfo(String petId) {
        return wrapGet("http://petcare-backend/api/pets/" + petId, "data");
    }

    private Result getRecords(String petId, Map<String, Object> args) {
        String end = (String) args.getOrDefault("endDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        String start = (String) args.getOrDefault("startDate",
                LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE));
        String url = String.format(
                "http://petcare-backend/api/activities/records/pet/%s?startDate=%s&endDate=%s&page=%s&size=%s",
                petId, start, end, args.getOrDefault("page", "0"), args.getOrDefault("size", "20"));
        return wrapGet(url, null);
    }

    private Result createRecord(String petId, Map<String, Object> args) {
        String activityName = (String) args.get("activityName");
        String description = (String) args.getOrDefault("description", "");
        String date = (String) args.getOrDefault("date",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Map<String, Object> petInfo = getPetInfo(petId).data();
        Object userId = petInfo.get("userId");
        if (userId == null) return new Result(Map.of("error", "找不到宠物信息"));

        String actUrl = "http://petcare-backend/api/activities/user/" + userId;
        try {
            JsonNode acts = lb.getForObject(actUrl, JsonNode.class);
            if (acts != null && acts.isArray()) {
                for (JsonNode a : acts) {
                    String name = a.get("activityName").asText();
                    if (name.contains(activityName) || activityName.contains(name)) {
                        String aid = a.get("activityId").asText();
                        String createUrl = String.format(
                                "http://petcare-backend/api/activities/records/pet/%s?activityId=%s&description=%s&date=%s&userId=%s",
                                petId, aid, URLEncoder.encode(description, StandardCharsets.UTF_8),
                                URLEncoder.encode(date, StandardCharsets.UTF_8), userId);
                        JsonNode r = lb.postForObject(createUrl, null, JsonNode.class);
                        return new Result(r != null
                                ? Map.of("success", true, "result", r.toString())
                                : Map.of("error", "创建失败"));
                    }
                }
            }
        } catch (Exception e) {
            return new Result(Map.of("error", "创建活动记录失败: " + e.getMessage()));
        }
        return new Result(Map.of("error", "未找到匹配的活动'" + activityName + "'，请先在宠物设置中添加该活动"));
    }

    private Result searchKnowledge(Map<String, Object> args) {
        String query = (String) args.get("query");
        return new Result(Map.of("result", "知识库已集成到活动健康分析中。关于'" + query + "'的建议，请使用 activityHealthAnalysis 接口。"));
    }

    private Result wrapGet(String url, String dataField) {
        try {
            JsonNode r = lb.getForObject(url, JsonNode.class);
            if (r == null) return new Result(Map.of("error", "请求失败"));
            if (dataField != null && r.has(dataField)) {
                JsonNode data = r.get(dataField);
                if (data.isObject()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    data.fieldNames().forEachRemaining(f -> m.put(f, data.get(f).isValueNode() ? data.get(f).asText() : data.get(f).toString()));
                    return new Result(m);
                }
                return new Result(Map.of("data", data.toString()));
            }
            if (r.isArray()) return new Result(Map.of("data", r.toString()));
            Map<String, Object> m = new LinkedHashMap<>();
            r.fieldNames().forEachRemaining(f -> m.put(f, r.get(f).isValueNode() ? r.get(f).asText() : r.get(f).toString()));
            return new Result(m);
        } catch (Exception e) {
            return new Result(Map.of("error", e.getMessage()));
        }
    }

    public record Result(Map<String, Object> data, String text) {
        public Result { data = data != null ? data : Map.of(); text = text != null ? text : data.toString(); }
        public boolean success() { return !data.containsKey("error"); }
        public Result(Map<String, Object> data) { this(data, null); }
    }
}
