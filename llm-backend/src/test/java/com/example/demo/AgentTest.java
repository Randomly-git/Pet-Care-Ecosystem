package com.example.demo;

import com.example.demo.agent.dto.AgentResult;
import com.example.demo.agent.resolver.AgentResolver;
import com.example.demo.agent.service.ToolExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AgentTest {

    @Autowired
    private ToolExecutor toolExecutor;

    @Autowired
    private AgentResolver agentResolver;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PET_ID = "4";

    @Test
    void testToolGetPetInfo() {
        System.out.println("\n========== 工具: get_pet_info ==========");
        ToolExecutor.Result result = toolExecutor.execute("get_pet_info", "{\"petId\":\"" + PET_ID + "\"}", PET_ID);
        System.out.println("成功: " + result.success());
        System.out.println("结果: " + result.text());
        assertTrue(result.success());
        assertTrue(result.text().contains("可乐") || result.text().contains("name"));
    }

    @Test
    void testToolGetActivityRecords() {
        System.out.println("\n========== 工具: get_activity_records ==========");
        String args = "{\"petId\":\"" + PET_ID + "\",\"startDate\":\"2025-01-01\",\"endDate\":\"2026-12-31\",\"size\":\"10\"}";
        ToolExecutor.Result result = toolExecutor.execute("get_activity_records", args, PET_ID);
        System.out.println("成功: " + result.success());
        System.out.println("结果: " + result.text());
        assertTrue(result.success());
    }

    @Test
    void testToolGetHealthAnalysis() {
        System.out.println("\n========== 工具: get_health_analysis ==========");
        ToolExecutor.Result result = toolExecutor.execute("get_health_analysis", "{\"petId\":\"" + PET_ID + "\"}", PET_ID);
        System.out.println("成功: " + result.success());
        System.out.println("结果: " + result.text());
        assertTrue(result.success());
        assertTrue(result.text().contains("activityHealthAnalysis"));
    }

    @Test
    void testToolSearchKnowledge() {
        System.out.println("\n========== 工具: search_knowledge ==========");
        ToolExecutor.Result result = toolExecutor.execute("search_knowledge", "{\"query\":\"边牧养护\"}", PET_ID);
        System.out.println("成功: " + result.success());
        System.out.println("结果: " + result.text());
        assertTrue(result.success());
    }

    @Test
    void testToolUnknown() {
        System.out.println("\n========== 工具: 未知工具 ==========");
        ToolExecutor.Result result = toolExecutor.execute("unknown_tool", "{}", PET_ID);
        System.out.println("成功: " + result.success());
        System.out.println("结果: " + result.text());
        assertFalse(result.success());
        assertTrue(result.text().contains("未知工具"));
    }

    @Test
    void testAgentSimpleQuery() {
        System.out.println("\n========== Agent: 简单问候 ==========");
        AgentResult result = agentResolver.aiAgent(PET_ID, "你好");
        System.out.println("宠物名: " + result.getPetName());
        System.out.println("回复: " + result.getMessage());
        System.out.println("工具调用数: " + (result.getToolCalls() != null ? result.getToolCalls().size() : 0));
        assertNotNull(result.getMessage());
    }

    @Test
    void testAgentCreateRecord() {
        System.out.println("\n========== Agent: 创建活动记录(仅列出参数) ==========");
        AgentResult result = agentResolver.aiAgent(PET_ID, "记录散步下午5点");
        System.out.println("回复: " + result.getMessage());
        if (result.getToolCalls() != null && !result.getToolCalls().isEmpty()) {
            for (var tc : result.getToolCalls()) {
                System.out.println("  工具: " + tc.getToolName() + " 参数: " + tc.getArguments() + " 成功: " + tc.isSuccess());
            }
        }
    }
}
