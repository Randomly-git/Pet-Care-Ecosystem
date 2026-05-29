package com.example.demo.agent.service;

import com.example.demo.agent.dto.AgentResult;
import com.example.demo.common.service.LlmService;
import com.example.demo.common.service.LlmService.QwenResponse;
import com.example.demo.common.service.LlmService.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AgentService {

    private final LlmService llmService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RestTemplate lb;

    public AgentService(LlmService llmService, ToolRegistry toolRegistry,
                        ToolExecutor toolExecutor,
                        @Qualifier("loadBalancedRestTemplate") RestTemplate lb) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.lb = lb;
    }

    public AgentResult process(String petId, String message) {
        log.info("Agent: petId={}, message={}", petId, message);

        Map<String, Object> petInfo = getPetInfo(petId);
        String petName = (String) petInfo.getOrDefault("name", "宠物");

        String systemPrompt = buildSystemPrompt(petId, petName);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", message));

        List<Map<String, Object>> tools = toolRegistry.getAllTools();

        List<AgentResult.ToolCallLog> logs = new ArrayList<>();

        QwenResponse response = llmService.callQwenWithTools(messages, tools);

        if (response.hasToolCalls()) {
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
                    finalMsg = "已调用" + last.getToolName() + "，操作成功。";
                } else {
                    finalMsg = "操作失败：" + last.getResult();
                }
            }

            AgentResult result = new AgentResult();
            result.setPetId(petId);
            result.setPetName(petName);
            result.setMessage(finalMsg);
            result.setToolCalls(logs);
            return result;
        }

        AgentResult result = new AgentResult();
        result.setPetId(petId);
        result.setPetName(petName);

        String content = response.getContent();
        if (content != null && (content.contains("petId") || content.contains("宠物ID") || content.contains("哪个宠物"))) {
            result.setMessage( content);
        } else if (content != null) {
            result.setMessage(content);
        } else {
            result.setMessage("请告诉我您想做什么？例如：\n- 查看宠物的活动记录\n- 记录一次新的活动\n- 获取健康分析");
        }
        result.setToolCalls(logs);
        return result;
    }

    private String buildSystemPrompt(String petId, String petName) {
        return "你是宠物" + petName + "的AI助手。\n\n"
                + "你可以使用以下工具来帮用户管理宠物。每次对话独立处理，不依赖之前的上下文。\n\n"
                + "可用工具：\n"
                + "1. get_pet_info - 查看宠物信息\n"
                + "2. get_activity_records - 查看活动记录（需要日期范围，缺就问用户）\n"
                + "3. create_activity_record - 记录新活动\n"
                + "4. get_health_analysis - AI健康分析\n"
                + "5. search_knowledge - 查养护知识\n\n"
                + "【创建活动记录的严格流程】\n"
                + "当用户想要记录活动时（如\"记录散步\"\"记一下喂食\"等），必须按以下步骤：\n"
                + "步骤1：检查用户是否提供了活动名称。如果没提供，问用户\"要记录什么活动？\"\n"
                + "步骤2：检查是否提供了活动时间。如果没提供，问用户\"请问是几点？\"\n"
                + "步骤3：检查是否提供了活动描述。如果没提供，问用户\"有什么要备注的吗？\"\n"
                + "步骤4：所有信息齐全后，用自然语言列出完整信息让用户确认（格式见下方示例）\n"
                + "步骤5：用户确认后（如用户说\"确认\"\"执行\"\"可以\"等），才调用create_activity_record工具\n\n"
                + "示例：\n"
                + "用户：\"记录散步\"\n"
                + "AI：\"请问是几点散步的？\"\n"
                + "用户：\"下午5点\"\n"
                + "AI：\"有什么要备注的吗？\"\n"
                + "用户：\"在小区遛了30分钟\"\n"
                + "AI：\"请确认：为" + petName + "记录[散步]（下午5点，在小区遛了30分钟），是否执行？\"\n"
                + "用户：\"执行\"\n"
                + "AI：→ 调用create_activity_record工具 → \"已记录成功✅\"\n\n"
                + "如果用户一次性提供了所有信息，直接跳到步骤4展示确认信息。\n"
                + "如果用户说\"不用了\"\"算了\"等，回复\"好的，已取消\"，不调用任何工具。\n\n"
                + "其他规则：\n"
                + "- 查看信息或搜索知识时，直接调用对应工具\n"
                + "- 回复要简短友好，使用宠物的名字：" + petName;
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
