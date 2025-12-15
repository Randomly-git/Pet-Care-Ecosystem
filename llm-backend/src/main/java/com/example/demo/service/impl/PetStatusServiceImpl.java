
package com.example.demo.service.impl;

import com.example.demo.client.PetServiceClient;
import com.example.demo.dto.request.PetStatusSummaryRequest;
import com.example.demo.dto.response.PetStatusSummaryResponse;
import com.example.demo.dto.response.StatusRecordDTO;
import com.example.demo.service.PetStatusService;
import com.example.demo.service.QwenService;
import com.example.demo.util.PromptBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetStatusServiceImpl implements PetStatusService {

    private final PetServiceClient petServiceClient;
    private final QwenService qwenService;

    public PetStatusServiceImpl(PetServiceClient petServiceClient, QwenService qwenService) {
        this.petServiceClient = petServiceClient;
        this.qwenService = qwenService;
    }

    @Override
    public PetStatusSummaryResponse generateStatusSummary(PetStatusSummaryRequest request) {
        System.out.println("开始生成宠物" + request.getPetId() + "的状态总结，类型: " + request.getSummaryType());

        // 1. 获取原始状态记录
        List<StatusRecordDTO> statusRecords = petServiceClient.getPetStatusRecords(request.getPetId());

        if (statusRecords.isEmpty()) {
            System.out.println("未找到宠物状态记录");
            return createEmptyResponse(request);
        }

        // 2. 构建prompt
        String prompt = PromptBuilder.buildStatusSummaryPrompt(statusRecords, request);
        System.out.println("构建的prompt长度: " + prompt.length());

        // 3. 调用通义千问API
        String aiResponse = qwenService.askQuestion(prompt);
        System.out.println("AI响应长度: " + aiResponse.length());

        // 4. 构建响应
        PetStatusSummaryResponse response = new PetStatusSummaryResponse();
        response.setPetId(request.getPetId());
        response.setPetName(request.getPetName() != null ? request.getPetName() : "未命名宠物");
        response.setSummary(aiResponse);
        response.setGeneratedAt(LocalDateTime.now());

        // 5. 尝试从AI响应中提取时间线和建议（简单实现）
        if (request.isIncludeTimeline()) {
            response.setTimeline(extractTimelineFromResponse(aiResponse));
        }

        if (request.isIncludeRecommendations()) {
            response.setRecommendations(extractRecommendationsFromResponse(aiResponse));
        }

        System.out.println("状态总结生成完成");
        return response;
    }

    @Override
    public PetStatusSummaryResponse generateStatusSummary(Long petId) {
        PetStatusSummaryRequest request = new PetStatusSummaryRequest();
        request.setPetId(petId);
        request.setSummaryType(PetStatusSummaryRequest.SummaryType.DETAILED);

        return generateStatusSummary(request);
    }

    @Override
    public String getRawStatusRecords(Long petId) {
        List<StatusRecordDTO> records = petServiceClient.getPetStatusRecords(petId);

        // 简单格式化输出
        StringBuilder sb = new StringBuilder();
        sb.append("宠物ID: ").append(petId).append("\n");
        sb.append("状态记录数量: ").append(records.size()).append("\n\n");

        for (StatusRecordDTO record : records) {
            sb.append("记录ID: ").append(record.getStatusRecordId()).append("\n");
            sb.append("状态: ").append(record.getStatusName()).append("\n");
            sb.append("时间: ").append(record.getStartDate())
                    .append(" 至 ").append(record.getEndDate() != null ? record.getEndDate() : "至今").append("\n");
            sb.append("描述: ").append(record.getStatusDescription()).append("\n");
            sb.append("-".repeat(50)).append("\n");
        }

        return sb.toString();
    }

    private PetStatusSummaryResponse createEmptyResponse(PetStatusSummaryRequest request) {
        PetStatusSummaryResponse response = new PetStatusSummaryResponse();
        response.setPetId(request.getPetId());
        response.setPetName(request.getPetName() != null ? request.getPetName() : "未命名宠物");
        response.setSummary("未找到该宠物的状态记录数据。");
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    private String extractTimelineFromResponse(String response) {
        // 简单提取时间线信息（实际中可以更智能）
        if (response.contains("时间线") || response.contains("时间轴")) {
            int start = response.indexOf("时间线");
            if (start == -1) start = response.indexOf("时间轴");

            if (start != -1) {
                int end = response.indexOf("\n\n", start);
                if (end == -1) end = response.length();
                return response.substring(start, end);
            }
        }
        return "时间线信息未明确提取";
    }

    private String extractRecommendationsFromResponse(String response) {
        // 简单提取建议信息
        if (response.contains("建议") || response.contains("推荐")) {
            int start = Math.max(
                    response.lastIndexOf("建议"),
                    response.lastIndexOf("推荐")
            );

            if (start != -1) {
                return response.substring(start, response.length());
            }
        }
        return "建议信息未明确提取";
    }
}