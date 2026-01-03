package com.example.stats.controller;

import com.example.stats.dto.request.StatsQueryRequest;
import com.example.stats.dto.response.ActivityStatsResponse;
import com.example.stats.service.ActivityStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/stats/activity")
@Tag(name = "活动统计", description = "宠物活动数据统计分析")
public class ActivityStatsController {

    private final ActivityStatsService activityStatsService;

    public ActivityStatsController(ActivityStatsService activityStatsService) {
        this.activityStatsService = activityStatsService;
    }

    @PostMapping
    @Operation(summary = "获取活动统计数据（完整对象传参）", description = "通过请求体传递宠物ID、起止时间及周期进行查询")
    public ActivityStatsResponse getActivityStats(@RequestBody StatsQueryRequest request) {
        log.info("接收活动统计请求: {}", request);
        return activityStatsService.getActivityStats(request);
    }

    @GetMapping("/pet/{petId}")
    @Operation(summary = "获取宠物活动简报", description = "默认获取该宠物最近3个月的月度统计数据")
    public ActivityStatsResponse getActivityStats(
            @Parameter(description = "宠物ID") @PathVariable Long petId,
            @Parameter(description = "统计周期 (DAILY/WEEKLY/MONTHLY)")
            @RequestParam(defaultValue = "MONTHLY") StatsQueryRequest.StatsPeriod period) {
        log.info("获取宠物 {} 的{}统计", petId, period);
        return activityStatsService.getActivityStats(petId, period);
    }

    @GetMapping("/pet/{petId}/range")
    @Operation(summary = "获取指定范围的统计数据", description = "查询指定时间段内的活动趋势")
    public ActivityStatsResponse getActivityStatsInRange(
            @Parameter(description = "宠物ID") @PathVariable Long petId,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "统计周期") @RequestParam(defaultValue = "MONTHLY") StatsQueryRequest.StatsPeriod period) {

        StatsQueryRequest request = new StatsQueryRequest();
        request.setPetId(petId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPeriod(period);

        return activityStatsService.getActivityStats(request);
    }
}