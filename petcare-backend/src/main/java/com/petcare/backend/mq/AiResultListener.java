package com.petcare.backend.mq;

import com.petcare.backend.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor // 自动构造注入 ActivityService
public class AiResultListener {

    private final ActivityService activityService;

    /**
     * 监听来自 Python BERT 服务的分析结果
     */
    @RabbitListener(queues = "pet_analysis_result_queue")
    public void handleAiResult(Map<String, Object> result) {
        try {
            // 1. 提取数据
            Long recordId = Long.valueOf(result.get("activityRecordId").toString());
            String label = (String) result.get("label");
            Double confidence = (Double) result.get("confidence");

            log.info("📥 [AI 结果接收] 记录ID: {}, 原始标签: {}, 置信度: {}", recordId, label, confidence);

            // 2. 核心逻辑判断
            Integer finalResultCode;

            // 如果置信度小于 60% 或者 标签本身就是 Normal，按正常(0)处理
            if (confidence < 0.6 || "Normal".equalsIgnoreCase(label)) {
                log.info("ℹ️ 记录 {} 置信度较低({} < 0.6) 或判定为 Normal，标记为正常状态(0)", recordId, confidence);
                finalResultCode = 0;
            } else {
                // 转换为对应的异常编码 (1-5)
                finalResultCode = mapLabelToCode(label);
                log.warn("⚠️ 记录 {} 检测到健康风险: {} (Code: {})", recordId, label, finalResultCode);
            }

            // 3. 调用 Service 更新数据库
            activityService.updateBertResult(recordId, finalResultCode);

        } catch (Exception e) {
            log.error("❌ 处理 AI 分析结果时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 将 Python 传回的英文标签映射为数据库要求的 1-5 数字
     */
    private Integer mapLabelToCode(String label) {
        if (label == null) return 0;
        return switch (label) {
            case "Digestive Issues"   -> 1;
            case "Parasites"          -> 2;
            case "Skin Irritations"   -> 3;
            case "Mobility Problems"  -> 4;
            case "Ear Infections"     -> 5;
            default -> 0; // 无法识别的标签统一归为正常
        };
    }
}