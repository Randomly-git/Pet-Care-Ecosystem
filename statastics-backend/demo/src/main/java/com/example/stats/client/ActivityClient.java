package com.example.stats.client;

import com.example.stats.dto.response.ActivityRecordDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
public class ActivityClient {

    private final RestTemplate restTemplate;

    // 使用活动服务期望的日期时间格式
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME; // 格式: "2025-10-01T00:00:00"

    public ActivityClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ActivityRecordDTO> getActivityRecords(Long petId, Integer activityKindId,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        try {
            // 构建URL
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://localhost:8082/api/activities/records/pet/").append(petId);

            boolean hasParam = false;

            if (activityKindId != null) {
                urlBuilder.append("?activityKindId=").append(activityKindId);
                hasParam = true;
            }

            if (startDate != null) {
                String formattedStartDate = formatDateTime(startDate);
                urlBuilder.append(hasParam ? "&" : "?")
                        .append("startDate=").append(formattedStartDate);
                hasParam = true;
            }

            if (endDate != null) {
                String formattedEndDate = formatDateTime(endDate);
                urlBuilder.append(hasParam ? "&" : "?")
                        .append("endDate=").append(formattedEndDate);
            }

            String url = urlBuilder.toString();
            System.out.println("调用活动服务URL: " + url);

            // 发送请求
            ResponseEntity<List<ActivityRecordDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ActivityRecordDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                System.out.println("获取活动记录失败，状态码: " + response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            System.out.println("调用活动服务异常: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 格式化日期时间为活动服务期望的格式
     * 根据你的活动服务日志，期望格式可能是: "2025-10-01T00:00:00"
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        // 尝试几种常见格式
        try {
            // 格式1: ISO格式 (2025-10-01T00:00:00)
            return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // 如果不行，可以尝试其他格式：
            // 格式2: 带毫秒 (2025-10-01T00:00:00.000)
            // return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));

            // 格式3: 不带秒 (2025-10-01T00:00)
            // return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (Exception e) {
            System.err.println("日期格式转换错误: " + e.getMessage());
            return dateTime.toString(); // 回退到默认格式
        }
    }

    /**
     * URL编码（处理特殊字符）
     */
    private String encodeUrlParam(String value) {
        if (value == null) return "";

        // 对日期时间字符串进行URL编码
        return value.replace(":", "%3A")
                .replace(" ", "%20")
                .replace("+", "%2B");
    }
}