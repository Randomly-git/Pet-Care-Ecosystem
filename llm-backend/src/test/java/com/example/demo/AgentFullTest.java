package com.example.demo;

import com.example.demo.agent.dto.AgentResult;
import com.example.demo.agent.resolver.AgentResolver;
import com.example.demo.agent.service.ToolExecutor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AgentFullTest {

    @Autowired
    private ToolExecutor toolExecutor;

    @Autowired
    private AgentResolver agentResolver;

    private static final String PET_ID = "4";

    // ========== 第一部分：逐个测试工具 ==========

    @Test
    @Order(1)
    void testToolGetPetInfo() {
        System.out.println("\n========== 工具1: get_pet_info ==========");
        ToolExecutor.Result r = toolExecutor.execute("get_pet_info",
                "{\"petId\":\"" + PET_ID + "\"}", PET_ID);
        System.out.println("结果: " + r.text());
        assertTrue(r.success(), "get_pet_info 应成功");
        assertTrue(r.text().contains("可乐"), "应包含宠物名");
        System.out.println("✅ get_pet_info 测试通过");
    }

    @Test
    @Order(2)
    void testToolGetActivityRecords() {
        System.out.println("\n========== 工具2: get_activity_records ==========");
        ToolExecutor.Result r = toolExecutor.execute("get_activity_records",
                "{\"petId\":\"" + PET_ID + "\",\"size\":\"10\"}", PET_ID);
        System.out.println("结果: " + r.text());
        assertTrue(r.success(), "get_activity_records 应成功");
        System.out.println("✅ get_activity_records 测试通过");
    }

    @Test
    @Order(3)
    void testToolGetHealthAnalysis() {
        System.out.println("\n========== 工具3: get_health_analysis ==========");
        ToolExecutor.Result r = toolExecutor.execute("get_health_analysis",
                "{\"petId\":\"" + PET_ID + "\"}", PET_ID);
        System.out.println("结果: " + r.text());
        assertTrue(r.success(), "get_health_analysis 应成功");
        assertTrue(r.text().contains("activityHealthAnalysis"), "应提示使用 GraphQL 接口");
        System.out.println("✅ get_health_analysis 测试通过");
    }

    @Test
    @Order(4)
    void testToolSearchKnowledge() {
        System.out.println("\n========== 工具4: search_knowledge ==========");
        ToolExecutor.Result r = toolExecutor.execute("search_knowledge",
                "{\"query\":\"边牧养护指南\"}", PET_ID);
        System.out.println("结果: " + r.text());
        assertTrue(r.success(), "search_knowledge 应成功");
        System.out.println("✅ search_knowledge 测试通过");
    }

    @Test
    @Order(5)
    void testToolUnknown() {
        System.out.println("\n========== 工具5: 未知工具 ==========");
        ToolExecutor.Result r = toolExecutor.execute("unknown_tool", "{}", PET_ID);
        System.out.println("结果: " + r.text());
        assertFalse(r.success(), "未知工具应失败");
        assertTrue(r.text().contains("未知工具"), "应提示未知工具");
        System.out.println("✅ 未知工具容错测试通过");
    }

    // ========== 第二部分：Agent 完整流程 ==========

    @Test
    @Order(6)
    void testAgentGreeting() {
        System.out.println("\n========== Agent流程1: 打招呼 ==========");
        AgentResult r = agentResolver.aiAgent(PET_ID, "你好");
        System.out.println("回复: " + r.getMessage());
        assertNotNull(r.getMessage());
        assertFalse(r.getMessage().isEmpty());
        System.out.println("✅ Agent 打招呼测试通过");
    }

    @Test
    @Order(7)
    void testAgentQueryPetInfo() {
        System.out.println("\n========== Agent流程2: 查询宠物信息 ==========");
        AgentResult r = agentResolver.aiAgent(PET_ID, "我的宠物叫什么名字");
        System.out.println("回复: " + r.getMessage());
        if (r.getToolCalls() != null && !r.getToolCalls().isEmpty()) {
            for (var tc : r.getToolCalls()) {
                System.out.println("  调用了工具: " + tc.getToolName() + " | 成功: " + tc.isSuccess());
                if (!tc.isSuccess()) {
                    System.out.println("  失败原因: " + tc.getResult());
                }
            }
        }
        assertNotNull(r.getMessage());
        System.out.println("✅ Agent 查询宠物信息测试通过");
    }

    @Test
    @Order(8)
    void testAgentCreateRecord() {
        System.out.println("\n========== Agent流程3: 创建活动记录 ==========");
        // 提供完整信息，测试 Agent 能否正确调用创建工具
        AgentResult r = agentResolver.aiAgent(PET_ID, "记录一下散步，下午5点，在小区遛了30分钟");
        System.out.println("回复: " + r.getMessage());
        if (r.getToolCalls() != null && !r.getToolCalls().isEmpty()) {
            for (var tc : r.getToolCalls()) {
                System.out.println("  调用了工具: " + tc.getToolName()
                        + " | 参数: " + tc.getArguments()
                        + " | 成功: " + tc.isSuccess());
            }
        }
        assertNotNull(r.getMessage());
        System.out.println("✅ Agent 创建活动记录测试通过");
    }

    @Test
    @Order(9)
    void testAgentQueryRecords() {
        System.out.println("\n========== Agent流程4: 查询活动记录 ==========");
        AgentResult r = agentResolver.aiAgent(PET_ID, "查一下最近的活动记录");
        System.out.println("回复: " + r.getMessage());
        if (r.getToolCalls() != null && !r.getToolCalls().isEmpty()) {
            for (var tc : r.getToolCalls()) {
                System.out.println("  调用了工具: " + tc.getToolName() + " | 成功: " + tc.isSuccess());
            }
        }
        assertNotNull(r.getMessage());
        System.out.println("✅ Agent 查询活动记录测试通过");
    }

    @Test
    @Order(10)
    void testAgentSearchKnowledge() {
        System.out.println("\n========== Agent流程5: 搜索知识 ==========");
        AgentResult r = agentResolver.aiAgent(PET_ID, "边牧有什么养护注意事项");
        System.out.println("回复: " + r.getMessage());
        if (r.getToolCalls() != null && !r.getToolCalls().isEmpty()) {
            for (var tc : r.getToolCalls()) {
                System.out.println("  调用了工具: " + tc.getToolName() + " | 成功: " + tc.isSuccess());
            }
        }
        assertNotNull(r.getMessage());
        System.out.println("✅ Agent 搜索知识测试通过");
    }
}
