package com.example.stats.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class WeeklyStatsDTO {
    private String yearWeek;  // 格式：2025-W40
    private Integer weekNumber;  // 周数 1-53
    private String weekRange;  // 格式：10月01日-10月07日
    private Integer totalActivities;
    private Map<String, Integer> activityTypeCounts;
    private Map<String, Integer> activityKindCounts;

    public WeeklyStatsDTO(String yearWeek, Integer weekNumber, String weekRange) {
        this.yearWeek = yearWeek;
        this.weekNumber = weekNumber;
        this.weekRange = weekRange;
        this.totalActivities = 0;
    }
}