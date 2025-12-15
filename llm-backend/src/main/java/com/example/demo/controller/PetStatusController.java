package com.example.demo.controller;

import com.example.demo.dto.request.PetStatusSummaryRequest;
import com.example.demo.dto.response.PetStatusSummaryResponse;
import com.example.demo.service.PetStatusService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/llm/pet-status")
public class PetStatusController {

    private final PetStatusService petStatusService;

    public PetStatusController(PetStatusService petStatusService) {
        this.petStatusService = petStatusService;
    }

    @PostMapping("/summary")
    public PetStatusSummaryResponse generateStatusSummary(@RequestBody PetStatusSummaryRequest request) {
        System.out.println("接收到宠物状态总结请求: " + request.getPetId());
        return petStatusService.generateStatusSummary(request);
    }

    @GetMapping("/summary/{petId}")
    public PetStatusSummaryResponse generateStatusSummary(@PathVariable("petId") Long petId) {
        System.out.println("快速生成宠物" + petId + "状态总结");
        return petStatusService.generateStatusSummary(petId);
    }

    @GetMapping("/summary/{petId}/quick")
    public String generateQuickSummary(@PathVariable("petId") Long petId) {
        System.out.println("生成宠物" + petId + "快速总结");
        PetStatusSummaryResponse response = petStatusService.generateStatusSummary(petId);
        return response.getSummary();
    }

    @GetMapping("/records/{petId}")
    public String getRawStatusRecords(@PathVariable("petId") Long petId) {
        System.out.println("获取宠物" + petId + "原始状态记录");
        return petStatusService.getRawStatusRecords(petId);
    }

    @GetMapping("/test/{petId}")
    public String testConnection(@PathVariable("petId") Long petId) {
        try {
            String records = petStatusService.getRawStatusRecords(petId);
            return String.format("""
                ✅ 宠物状态总结服务测试成功
                宠物ID: %d
                当前时间: %s
                服务状态: 正常运行
                端口: 8086
                
                📋 原始记录预览:
                %s
                """,
                    petId,
                    java.time.LocalDateTime.now(),
                    records.length() > 200 ? records.substring(0, 200) + "..." : records
            );
        } catch (Exception e) {
            return String.format("""
                ❌ 服务连接失败
                宠物ID: %d
                错误信息: %s
                当前时间: %s
                """,
                    petId,
                    e.getMessage(),
                    java.time.LocalDateTime.now()
            );
        }
    }

    @GetMapping("/health")
    public String health() {
        return "✅ 宠物状态总结服务运行正常 (8086端口)";
    }
}