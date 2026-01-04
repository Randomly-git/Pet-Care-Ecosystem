package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PetHealthData {
    // 宠物基本信息
    private String petId;
    private String name;
    private String breed;
    private String species;

    // AI健康建议
    private String healthAdvice;

    // 健康状态记录
    private List<StatusRecord> statusRecords;



    /**
     * 宠物健康状态记录
     */
    @Data
    public static class StatusRecord {
        private String statusName;     // 状态名称（如：皮肤病、肥胖等）
        private String description;    // 状态描述
        private String startDate;      // 开始日期
    }


}