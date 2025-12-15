package com.example.demo.client;

import com.example.demo.config.QwenConfig;
import com.example.demo.dto.QwenRequest;
import com.example.demo.dto.QwenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class QwenApiClient {

    private final RestTemplate restTemplate;
    private final QwenConfig qwenConfig;
    private final ObjectMapper objectMapper;

    public QwenApiClient(QwenConfig qwenConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.qwenConfig = qwenConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public QwenResponse callQwenApi(String question) {
        try {
            // 构建请求
            QwenRequest request = new QwenRequest();
            request.setModel(qwenConfig.getModel());
            request.setMessages(List.of(
                    new QwenRequest.Message("user", question)
            ));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenConfig.getApiKey());

            HttpEntity<QwenRequest> entity = new HttpEntity<>(request, headers);

            // 发送请求
            String url = qwenConfig.getBaseUrl() + "/chat/completions";
            ResponseEntity<QwenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    QwenResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("调用通义千问API失败，状态码：{}", response.getStatusCode());
                throw new RuntimeException("调用API失败");
            }

        } catch (Exception e) {
            log.error("调用通义千问API异常", e);
            throw new RuntimeException("调用API异常", e);
        }
    }
}