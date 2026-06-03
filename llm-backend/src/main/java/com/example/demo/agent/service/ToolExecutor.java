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
import java.util.stream.Collectors;

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
                "http://petcare-backend/api/activities/records/pet/%s?startDate=%sT00:00:00&endDate=%sT23:59:59&page=%s&size=%s",
                petId, start, end, args.getOrDefault("page", "0"), args.getOrDefault("size", "20"));
        return wrapGet(url, null);
    }

    private Result createRecord(String petId, Map<String, Object> args) {
        String activityName = (String) args.get("activityName");
        if (activityName == null || activityName.isBlank()) {
            return new Result(Map.of("error", "请指定活动名称"));
        }
        activityName = activityName.trim();
        String description = (String) args.getOrDefault("description", "");
        String rawDate = (String) args.getOrDefault("date", "");
        String date;
        if (rawDate.contains("-") && rawDate.contains(":")) {
            date = rawDate.replace(" ", "T");
            if (date.chars().filter(c -> c == ':').count() == 1) date += ":00";
        } else if (!rawDate.isEmpty()) {
            date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T" + rawDate;
            if (date.chars().filter(c -> c == ':').count() == 1) date += ":00";
        } else {
            date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        Map<String, Object> petInfo = getPetInfo(petId).data();
        Object userId = petInfo.get("userId");
        if (userId == null) return new Result(Map.of("error", "找不到宠物信息"));

        String actUrl = "http://petcare-backend/api/activities/user/" + userId;
        try {
            JsonNode acts = lb.getForObject(actUrl, JsonNode.class);
            if (acts != null && acts.isArray()) {
                // 1. 尝试精确匹配
                for (JsonNode a : acts) {
                    if (a.get("activityName").asText().equals(activityName)) {
                        return executeCreateRecord(petId, a.get("activityId").asText(),
                                description, date, String.valueOf(userId));
                    }
                }
                // 2. 尝试模糊匹配，收集所有匹配项
                List<JsonNode> matches = new ArrayList<>();
                for (JsonNode a : acts) {
                    String name = a.get("activityName").asText();
                    if (name.contains(activityName) || activityName.contains(name)) {
                        matches.add(a);
                    }
                }
                if (matches.size() == 1) {
                    JsonNode a = matches.get(0);
                    return executeCreateRecord(petId, a.get("activityId").asText(),
                            description, date, String.valueOf(userId));
                } else if (matches.size() > 1) {
                    String names = matches.stream()
                            .map(a -> a.get("activityName").asText())
                            .collect(Collectors.joining("、"));
                    return new Result(Map.of("error", "找到多个匹配的活动（" + names + "），请指定具体名称"));
                }
            }
        } catch (Exception e) {
            return new Result(Map.of("error", "创建活动记录失败: " + e.getMessage()));
        }
        return new Result(Map.of("error", "未找到匹配的活动'" + activityName + "'，可用活动：请在宠物设置中添加"));
    }

    private Result executeCreateRecord(String petId, String activityId, String description,
                                        String date, String userId) {
        try {
            String createUrl = String.format(
                    "http://petcare-backend/api/activities/records/pet/%s?activityId=%s&description=%s&date=%s&userId=%s",
                    petId, activityId, URLEncoder.encode(description, StandardCharsets.UTF_8),
                    URLEncoder.encode(date, StandardCharsets.UTF_8), URLEncoder.encode(userId, StandardCharsets.UTF_8));
            JsonNode r = lb.postForObject(createUrl, null, JsonNode.class);
            return new Result(r != null
                    ? Map.of("success", true, "result", r.toString())
                    : Map.of("error", "创建失败"));
        } catch (Exception e) {
            return new Result(Map.of("error", "创建失败: " + e.getMessage()));
        }
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
