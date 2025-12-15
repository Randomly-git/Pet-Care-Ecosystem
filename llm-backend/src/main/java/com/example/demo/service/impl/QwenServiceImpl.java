package com.example.demo.service.impl;

import com.example.demo.client.QwenApiClient;
import com.example.demo.dto.QwenResponse;
import com.example.demo.service.QwenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QwenServiceImpl implements QwenService {

    private final QwenApiClient qwenApiClient;

    public QwenServiceImpl(QwenApiClient qwenApiClient) {
        this.qwenApiClient = qwenApiClient;
    }

    @Override
    public String askQuestion(String question) {
        log.info("收到问题：{}", question);

        try {
            QwenResponse response = qwenApiClient.callQwenApi(question);
            String answer = response.getAnswer();

            log.info("获取到回答，长度：{}", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("处理问题失败：{}", question, e);
            return "抱歉，处理您的问题时出现错误：" + e.getMessage();
        }
    }
}