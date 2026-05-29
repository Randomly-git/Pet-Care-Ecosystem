package com.example.demo.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class LlmService {

    private final RestTemplate normalRestTemplate;
    private final ObjectMapper objectMapper;

    private static final String QWEN_API_KEY = "sk-972a298500904e809a415aa9f153caac";
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public LlmService(
            @Qualifier("normalRestTemplate") RestTemplate normalRestTemplate,
            ObjectMapper objectMapper) {
        this.normalRestTemplate = normalRestTemplate;
        this.objectMapper = objectMapper;
    }

    public String callQwenAI(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "qwen3-max");
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        try {
            JsonNode resp = doPost(request);
            if (resp != null && resp.has("choices")) {
                JsonNode msg = resp.get("choices").get(0).get("message");
                if (msg != null && msg.has("content") && !msg.get("content").isNull()) {
                    return msg.get("content").asText();
                }
            }
        } catch (Exception e) {
            log.error("Qwen调用失败", e);
        }
        return "AI服务暂时不可用。";
    }

    public QwenResponse callQwenWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "qwen3-max");
        request.put("messages", messages);
        if (tools != null && !tools.isEmpty()) request.put("tools", tools);
        try {
            JsonNode resp = doPost(request);
            if (resp != null && resp.has("choices")) {
                JsonNode msg = resp.get("choices").get(0).get("message");
                QwenResponse result = new QwenResponse();
                if (msg.has("content") && !msg.get("content").isNull()) {
                    result.setContent(msg.get("content").asText());
                }
                if (msg.has("tool_calls") && msg.get("tool_calls").isArray()) {
                    List<ToolCall> calls = new ArrayList<>();
                    for (JsonNode tc : msg.get("tool_calls")) {
                        ToolCall call = new ToolCall();
                        call.setId(tc.get("id").asText());
                        call.setName(tc.get("function").get("name").asText());
                        call.setArguments(tc.get("function").get("arguments").asText());
                        calls.add(call);
                    }
                    result.setToolCalls(calls);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Qwen工具调用失败", e);
        }
        QwenResponse err = new QwenResponse();
        err.setContent("AI服务暂时不可用。");
        return err;
    }

    public QwenResponse callQwenVision(String textPrompt, String imageUrl) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
        content.add(Map.of("type", "text", "text", textPrompt));
        Map<String, Object> request = new HashMap<>();
        request.put("model", "qwen-vl-max");
        request.put("messages", List.of(Map.of("role", "user", "content", content)));
        try {
            JsonNode resp = doPost(request);
            if (resp != null && resp.has("choices")) {
                JsonNode msg = resp.get("choices").get(0).get("message");
                QwenResponse result = new QwenResponse();
                if (msg.has("content") && !msg.get("content").isNull()) {
                    result.setContent(msg.get("content").asText());
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Qwen VLM调用失败", e);
        }
        QwenResponse err = new QwenResponse();
        err.setContent("图像识别服务暂时不可用。");
        return err;
    }

    private JsonNode doPost(Map<String, Object> requestBody) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + QWEN_API_KEY);
        headers.set("Content-Type", "application/json");
        org.springframework.http.HttpEntity<Map<String, Object>> entity =
                new org.springframework.http.HttpEntity<>(requestBody, headers);
        return normalRestTemplate.postForObject(QWEN_API_URL, entity, JsonNode.class);
    }

    @Data
    public static class QwenResponse {
        private String content;
        private List<ToolCall> toolCalls;
        public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    }

    @Data
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments;
    }
}
