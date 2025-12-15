package com.example.stats.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class MonthlyStatsDTO {
    private String yearMonth;  // 格式：2025-10
    private Integer totalActivities;  // 该月总活动次数
    private Map<String, Integer> activityTypeCounts;  // 各活动类型次数
    private Map<String, Integer> activityKindCounts;  // 各活动大类次数

    public MonthlyStatsDTO(String yearMonth) {
        this.yearMonth = yearMonth;
        this.totalActivities = 0;
    }
}
