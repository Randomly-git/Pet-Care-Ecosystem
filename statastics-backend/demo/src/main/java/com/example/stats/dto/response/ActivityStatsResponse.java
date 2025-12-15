package com.example.stats.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ActivityStatsResponse {
    private Long petId;
    private String period;  // MONTHLY 或 WEEKLY
    private Integer totalActivities;  // 总活动次数
    private Integer uniqueActivityTypes;  // 活动类型数量
    private Integer uniqueActivityKinds;  // 活动大类数量
    private List<MonthlyStatsDTO> monthlyStats;  // 按月统计数据
    private List<WeeklyStatsDTO> weeklyStats;  // 按周统计数据

    // 热门活动类型
    private String mostFrequentActivity;
    private Integer mostFrequentCount;

    // 热门活动大类
    private String mostFrequentKind;
    private Integer mostFrequentKindCount;
}
