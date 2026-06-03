package com.example.demo.agent.service;

import com.example.demo.agent.dto.AgentResult;
import com.example.demo.common.service.LlmService;
import com.example.demo.common.service.LlmService.QwenResponse;
import com.example.demo.common.service.LlmService.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AgentService {

    private final LlmService llmService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RestTemplate lb;

    /** 会话缓存: conversationId → ConversationSession */
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    private static final long SESSION_TTL_MINUTES = 30;

    public AgentService(LlmService llmService, ToolRegistry toolRegistry,
                        ToolExecutor toolExecutor,
                        @Qualifier("loadBalancedRestTemplate") RestTemplate lb) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.lb = lb;
    }

    @PostConstruct
    void startCleaner() {
        cleaner.scheduleAtFixedRate(this::cleanStaleSessions, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanStaleSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e ->
                (now - e.getValue().lastAccessTime) > TimeUnit.MINUTES.toMillis(SESSION_TTL_MINUTES));
    }

    /** 一轮对话中的上下文 */
    private static class ConversationSession {
        String petId;
        List<Map<String, Object>> messages;
        long lastAccessTime;
    }

    public AgentResult process(String petId, String message, String conversationId) {
        log.info("Agent: petId={}, message={}, conversationId={}", petId, message, conversationId);

        Map<String, Object> petInfo = getPetInfo(petId);
        String petName = (String) petInfo.getOrDefault("name", "宠物");

        // 1. 查找或创建会话
        ConversationSession session;
        boolean isNew = false;
        if (conversationId == null || conversationId.isBlank() || !sessions.containsKey(conversationId)) {
            session = new ConversationSession();
            session.petId = petId;
            session.messages = new ArrayList<>();
            session.messages.add(Map.of("role", "system", "content", buildSystemPrompt(petId, petName)));
            conversationId = UUID.randomUUID().toString();
            isNew = true;
        } else {
            session = sessions.get(conversationId);
            // 如果 petId 不匹配则重置
            if (!petId.equals(session.petId)) {
                session.messages.clear();
                session.messages.add(Map.of("role", "system", "content", buildSystemPrompt(petId, petName)));
                session.petId = petId;
            }
        }

        // 2. 追加用户消息
        session.messages.add(Map.of("role", "user", "content", message));
        session.lastAccessTime = System.currentTimeMillis();

        // 3. 调用 Qwen
        List<Map<String, Object>> tools = toolRegistry.getAllTools();
        QwenResponse response = llmService.callQwenWithTools(session.messages, tools);

        List<AgentResult.ToolCallLog> logs = new ArrayList<>();

        if (response.hasToolCalls()) {
            // 参数齐全 + 已确认 → 执行工具
            for (ToolCall tc : response.getToolCalls()) {
                log.info("工具调用: {} ({})", tc.getName(), tc.getArguments());
                ToolExecutor.Result result = toolExecutor.execute(tc.getName(), tc.getArguments(), petId);
                AgentResult.ToolCallLog logEntry = new AgentResult.ToolCallLog();
                logEntry.setToolName(tc.getName());
                logEntry.setArguments(tc.getArguments());
                logEntry.setResult(result.text());
                logEntry.setSuccess(result.success());
                logs.add(logEntry);
            }

            String finalMsg;
            if (logs.isEmpty()) {
                finalMsg = response.getContent() != null ? response.getContent() : "处理完成。";
            } else {
                AgentResult.ToolCallLog last = logs.get(logs.size() - 1);
                if (last.isSuccess()) {
                    finalMsg = last.getResult();
                } else {
                    finalMsg = "操作失败：" + last.getResult();
                }
            }

            // 执行完毕 → 清空会话
            sessions.remove(conversationId);

            AgentResult result = new AgentResult();
            result.setPetId(petId);
            result.setPetName(petName);
            result.setMessage(finalMsg);
            result.setConversationId(null);
            result.setToolCalls(logs);
            return result;
        }

        // 4. 无 tool_call — AI 还在问参数或已取消
        String content = response.getContent() != null ? response.getContent() : "";

        // 追加 AI 回复到历史
        session.messages.add(Map.of("role", "assistant", "content", content));
        session.lastAccessTime = System.currentTimeMillis();
        sessions.put(conversationId, session);

        // 检测取消
        boolean isCancelled = content.contains("取消") || content.contains("已取消");

        AgentResult result = new AgentResult();
        result.setPetId(petId);
        result.setPetName(petName);
        result.setMessage(content);
        result.setConversationId(isCancelled ? null : conversationId);
        result.setToolCalls(logs);

        if (isCancelled) {
            sessions.remove(conversationId);
        }

        return result;
    }

    private String buildSystemPrompt(String petId, String petName) {
        return "你是宠物" + petName + "的AI助手。\n\n"
                + "你可以使用以下3个工具来帮用户管理宠物。\n\n"
                + "可用工具：\n"
                + "1. get_pet_info - 查看宠物信息（无需参数，直接调用）\n"
                + "2. get_activity_records - 查看活动记录（缺日期就问用户，问齐后直接调用）\n"
                + "3. create_activity_record - 记录新活动（严格的多步确认流程，见下方）\n\n"
                + "【create_activity_record 严格流程】\n"
                + "当用户想记录活动时（如\"记录散步\"\"记一下喂食\"），必须按：\n"
                + "步骤1：检查活动名称。没提供就问\"要记录什么活动？\"\n"
                + "步骤2：检查活动时间。没提供就问\"请问是几点？\"\n"
                + "步骤3：检查活动描述。没提供就问\"有什么要备注的吗？\"\n"
                + "步骤4：信息齐全 → 列出完整信息让用户确认\n"
                + "步骤5：用户确认后才调用create_activity_record\n\n"
                + "【关键：调用create_activity_record时参数必须全部提供】\n"
                + " - petId: 必须是数字（" + petId + "）\n"
                + " - activityName: 活动名称（如\"散步\"\"喂食\"\"洗澡\"等）\n"
                + " - description: 活动备注（可为空字符串）\n"
                + " - date: 活动时间（格式 yyyy-MM-ddTHH:mm:ss，如\"2026-06-03T17:00:00\"，只提供时间如\"17:00\"也会被自动补全为当天日期）\n\n"
                + "示例：\n"
                + "用户：\"记录散步\"\n"
                + "AI：\"请问是几点散步的？\"\n"
                + "用户：\"下午5点\"\n"
                + "AI：\"有什么要备注的吗？\"\n"
                + "用户：\"在小区遛了30分钟\"\n"
                + "AI：\"请确认：为" + petName + "记录[散步]（下午5点，在小区遛了30分钟），是否执行？\"\n"
                + "用户：\"执行\" → 调create_activity_record → \"已记录✅\"\n\n"
                + "一次性提供全部信息则直接跳到步骤4。\n"
                + "用户说\"不用了\"\"算了\" → 回复\"好的，已取消\"，不调任何工具。\n\n"
                + "注意：所有工具的 petId 参数必须使用数字ID（" + petId + "），不能使用宠物名称。\n"
                + "回复要简短友好，使用宠物的名字：" + petName;
    }

    private Map<String, Object> getPetInfo(String petId) {
        String url = "http://petcare-backend/api/pets/" + petId;
        try {
            JsonNode r = lb.getForObject(url, JsonNode.class);
            if (r != null && r.get("success").asBoolean()) {
                JsonNode d = r.get("data");
                Map<String, Object> info = new HashMap<>();
                info.put("name", d.get("name").asText());
                info.put("breed", d.get("breed").asText());
                info.put("species", d.get("species").asText());
                return info;
            }
        } catch (Exception e) { log.warn("获取宠物信息失败", e); }
        Map<String, Object> f = new HashMap<>();
        f.put("name", "宠物"); f.put("breed", "未知"); f.put("species", "未知");
        return f;
    }
}