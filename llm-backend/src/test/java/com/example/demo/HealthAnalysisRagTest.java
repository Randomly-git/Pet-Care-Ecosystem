package com.example.demo;

import com.example.demo.analysis.dto.AnalysisResult;
import com.example.demo.analysis.resolver.AnalysisResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HealthAnalysisRagTest {

    @Autowired
    private AnalysisResolver analysisResolver;

    @Test
    void testHealthAnalysis() {
        System.out.println("\n========== 测试: RAG 健康分析 ==========");

        // 使用 "活了一百万次的猫" 账号的宠物 "可乐" (petId=4, 边牧, 6条记录)
        AnalysisResult result = analysisResolver.activityHealthAnalysis("4", 30, null);

        System.out.println("宠物名: " + result.getPetName());
        System.out.println("品种: " + result.getBreed());
        System.out.println("物种: " + result.getSpecies());
        System.out.println("分析类型: " + result.getAnalysisType());
        System.out.println("引用的知识来源: " + (result.getKnowledgeSources() != null ? result.getKnowledgeSources().size() : 0) + " 条");

        String analysis = result.getAnalysis();
        System.out.println("\n--- AI 分析结果片段 ---");
        System.out.println(analysis.length() > 500 ? analysis.substring(0, 500) + "..." : analysis);

        assertNotNull(analysis);
        assertFalse(analysis.contains("服务不可用"), "AI分析不应包含错误信息");
        assertFalse(analysis.isEmpty(), "AI分析不应为空");
        System.out.println("✅ RAG 健康分析测试通过");
    }

    @Test
    void testHealthAnalysisWithUserQuestion() {
        System.out.println("\n========== 测试: RAG 健康分析(带用户提问) ==========");

        AnalysisResult result = analysisResolver.activityHealthAnalysis("4", 30, "最近运动量够吗？");

        System.out.println("宠物名: " + result.getPetName());
        System.out.println("分析类型: " + result.getAnalysisType());
        System.out.println("知识来源: " + (result.getKnowledgeSources() != null ? result.getKnowledgeSources().size() : 0) + " 条");

        assertNotNull(result.getAnalysis());
        System.out.println("✅ 带提问的健康分析测试通过");
    }
}
