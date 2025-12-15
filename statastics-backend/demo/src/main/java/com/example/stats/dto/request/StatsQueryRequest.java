package com.example.stats.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StatsQueryRequest {
    private Long petId;
    private Integer activityKindId; // 可选，不传则统计所有类型
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private StatsPeriod period = StatsPeriod.MONTHLY; // 统计周期：MONTHLY, WEEKLY

    public enum StatsPeriod {
        MONTHLY,   // 按月统计
        WEEKLY     // 按周统计
    }
}