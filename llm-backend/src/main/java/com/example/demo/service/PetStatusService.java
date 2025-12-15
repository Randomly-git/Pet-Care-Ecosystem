package com.example.demo.service;

import com.example.demo.dto.request.PetStatusSummaryRequest;
import com.example.demo.dto.response.PetStatusSummaryResponse;

public interface PetStatusService {
    /**
     * 生成宠物状态总结
     */
    PetStatusSummaryResponse generateStatusSummary(PetStatusSummaryRequest request);

    /**
     * 快速生成宠物状态总结
     */
    PetStatusSummaryResponse generateStatusSummary(Long petId);

    /**
     * 获取原始状态记录
     */
    String getRawStatusRecords(Long petId);
}
