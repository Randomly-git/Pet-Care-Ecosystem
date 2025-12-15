package com.example.demo.controller;

import com.example.demo.service.QwenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "通义千问对话接口", description = "封装通义千问API的对话服务")
public class QwenController {

    private final QwenService qwenService;

    // 这里必须使用 QwenController，因为类名是 QwenController
    public QwenController(QwenService qwenService) {
        this.qwenService = qwenService;
    }

    @PostMapping("/ask")
    @Operation(summary = "提问接口", description = "向通义千问提问并获取回答")
    public String askQuestion(@RequestBody String question) {
        log.info("接收到提问请求");
        return qwenService.askQuestion(question);
    }

    @GetMapping("/ask")
    @Operation(summary = "GET方式提问", description = "通过GET参数方式提问")
    public String askQuestionByGet(@RequestParam String question) {
        log.info("接收到GET提问请求");
        return qwenService.askQuestion(question);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "服务健康状态检查")
    public String healthCheck() {
        return "Service is running!";
    }
}