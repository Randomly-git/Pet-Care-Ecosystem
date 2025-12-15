package com.example.demo.service;

public interface QwenService {
    /**
     * 调用通义千问API回答问题
     * @param question 用户问题
     * @return AI回答
     */
    String askQuestion(String question);
}