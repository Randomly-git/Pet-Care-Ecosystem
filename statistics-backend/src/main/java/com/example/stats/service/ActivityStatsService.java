package com.example.stats.service;

import com.example.stats.dto.request.StatsQueryRequest;
import com.example.stats.dto.response.ActivityStatsResponse;

public interface ActivityStatsService {
    /**
     * 获取活动统计数据
     */
    ActivityStatsResponse getActivityStats(StatsQueryRequest request);

    /**
     * 获取指定宠物的活动统计数据
     */
    ActivityStatsResponse getActivityStats(Long petId, StatsQueryRequest.StatsPeriod period);

    /**
     * 获取指定时间段的活动统计数据
     */
    ActivityStatsResponse getActivityStats(Long petId, String startDate, String endDate,
                                           StatsQueryRequest.StatsPeriod period);
}
