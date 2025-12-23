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

    // 1. 修改为微服务名称
    private final String activityServiceUrl = "http://petcare-backend";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // 2. 如果该微服务只有一个 RestTemplate Bean，直接注入即可
    public ActivityClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ActivityRecordDTO> getActivityRecords(Long petId, Integer activityKindId,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate) {
        try {
            // 3. 拼接 URL
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(activityServiceUrl).append("/api/activities/records/pet/").append(petId);

            boolean hasParam = false;
            if (activityKindId != null) {
                urlBuilder.append("?activityKindId=").append(activityKindId);
                hasParam = true;
            }

            if (startDate != null) {
                urlBuilder.append(hasParam ? "&" : "?")
                        .append("startDate=").append(formatDateTime(startDate));
                hasParam = true;
            }

            if (endDate != null) {
                urlBuilder.append(hasParam ? "&" : "?")
                        .append("endDate=").append(formatDateTime(endDate));
            }

            String url = urlBuilder.toString();
            System.out.println("调用活动服务URL: " + url);

            ResponseEntity<List<ActivityRecordDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ActivityRecordDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return Collections.emptyList();

        } catch (Exception e) {
            System.out.println("调用活动服务异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        // 注意：RestTemplate 发送 GET 请求时，会自动对参数值进行 URL 编码
        // 只要格式化正确即可
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}