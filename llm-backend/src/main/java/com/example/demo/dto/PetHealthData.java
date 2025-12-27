package com.example.demo.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PetHealthData {
    // 宠物基本信息
    private String petId;
    private String name;
    private String breed;
    private String species;

    // 百度搜索结果
    private List<Article> articles;

    // 健康状态记录
    private List<StatusRecord> statusRecords;

    // AI健康建议
    private String healthAdvice;

    // 相关链接
    private List<Link> relatedLinks;

    @Data
    public static class Article {
        private String title;
        private String url;
        private String snippet;
        private String source;
    }

    @Data
    public static class StatusRecord {
        private String statusName;
        private String description;
        private String startDate;
    }

    @Data
    public static class Link {
        private String title;
        private String url;
    }
}