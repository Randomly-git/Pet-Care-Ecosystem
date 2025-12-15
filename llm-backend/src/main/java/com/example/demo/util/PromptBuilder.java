package com.example.demo.util;

import com.example.demo.dto.request.PetStatusSummaryRequest;
import com.example.demo.dto.response.StatusRecordDTO;
import java.util.List;
import java.util.stream.Collectors;

public class PromptBuilder {

    public static String buildStatusSummaryPrompt(List<StatusRecordDTO> statusRecords,
                                                  PetStatusSummaryRequest request) {
        String petName = request.getPetName() != null ? request.getPetName() : "宠物";

        StringBuilder prompt = new StringBuilder();

        // 基本指令
        prompt.append("你是一位专业的宠物健康管理专家。请基于以下宠物状态记录数据，为宠物\"")
                .append(petName)
                .append("\"生成一份状态总结报告。\n\n");

        // 数据概览
        prompt.append("## 数据概览\n")
                .append("- 宠物ID: ").append(statusRecords.get(0).getPetId()).append("\n")
                .append("- 记录数量: ").append(statusRecords.size()).append(" 条\n")
                .append("- 时间范围: ").append(getDateRange(statusRecords)).append("\n")
                .append("- 状态类型: ").append(getStatusTypes(statusRecords)).append("\n\n");

        // 详细记录（按时间倒序）
        prompt.append("## 详细状态记录（从最近到最早）\n");

        List<StatusRecordDTO> sortedRecords = statusRecords.stream()
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedRecords.size(); i++) {
            StatusRecordDTO record = sortedRecords.get(i);
            prompt.append("\n### 记录 ").append(i + 1).append(":\n")
                    .append("- **状态**: ").append(record.getStatusName()).append("\n")
                    .append("- **时间**: ").append(record.getStartDate())
                    .append(" 至 ").append(record.getEndDate() != null ? record.getEndDate() : "至今").append("\n")
                    .append("- **描述**: ").append(record.getStatusDescription()).append("\n");

            if (record.getMediaCount() > 0) {
                prompt.append("- **媒体文件**: ").append(record.getMediaCount()).append(" 个\n");
            }
        }

        // 根据请求类型添加不同的指令
        prompt.append("\n## 报告要求\n");

        switch (request.getSummaryType()) {
            case CONCISE:
                prompt.append(buildConciseInstructions());
                break;
            case ANALYTICAL:
                prompt.append(buildAnalyticalInstructions());
                break;
            case DETAILED:
            default:
                prompt.append(buildDetailedInstructions());
        }

        // 可选部分
        if (request.isIncludeTimeline()) {
            prompt.append("\n4. **时间线图示**: 用文字描述时间线，突出关键节点\n");
        }

        if (request.isIncludeRecommendations()) {
            prompt.append("\n5. **建议**: 提供实用的养护建议\n");
        }

        prompt.append("\n请现在生成报告：");

        return prompt.toString();
    }

    private static String buildConciseInstructions() {
        return "请生成简洁的总结（300-400字），包含：\n" +
                "1. **总体概况**: 简要描述宠物的状态历程\n" +
                "2. **关键阶段**: 突出重要转变节点\n" +
                "3. **养护重点**: 分析主人的关注点\n";
    }

    private static String buildDetailedInstructions() {
        return "请生成详细的总结报告（800-1000字），包含：\n" +
                "1. **总体概况**: 概括宠物的整体状态变化轨迹\n" +
                "2. **阶段分析**: 按时间顺序分析各个阶段\n" +
                "3. **健康评估**: 评估饮食、环境、特殊状态等方面\n" +
                "4. **主人关怀**: 分析主人的养护措施和投入\n" +
                "5. **关键发现**: 总结主要成就和需要注意的问题\n";
    }

    private static String buildAnalyticalInstructions() {
        return "请生成专业的分析报告（1000-1200字），包含：\n" +
                "1. **阶段划分**: 科学划分生命阶段\n" +
                "2. **风险评估**: 识别潜在健康风险\n" +
                "3. **营养分析**: 详细分析饮食方案的科学性\n" +
                "4. **环境评估**: 评估居住条件的适宜性\n" +
                "5. **行为解读**: 分析状态描述中的行为信号\n" +
                "6. **专业建议**: 基于最佳实践的建议\n";
    }

    private static String getDateRange(List<StatusRecordDTO> records) {
        if (records.isEmpty()) return "无记录";

        String earliest = records.stream()
                .map(r -> r.getStartDate().toString())
                .min(String::compareTo)
                .orElse("未知");

        String latest = records.stream()
                .map(r -> r.getStartDate().toString())
                .max(String::compareTo)
                .orElse("未知");

        return earliest + " 至 " + latest;
    }

    private static String getStatusTypes(List<StatusRecordDTO> records) {
        return records.stream()
                .map(StatusRecordDTO::getStatusName)
                .distinct()
                .collect(Collectors.joining(", "));
    }
}