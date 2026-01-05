package com.example.stats.service.impl;

import com.example.stats.client.ActivityClient;
import com.example.stats.dto.request.StatsQueryRequest;
import com.example.stats.dto.response.*;
import com.example.stats.service.ActivityStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ActivityStatsServiceImpl implements ActivityStatsService {

    private final ActivityClient activityClient;

    // 日期格式化器
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日");
    private static final DateTimeFormatter WEEK_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-'W'ww");

    public ActivityStatsServiceImpl(ActivityClient activityClient) {
        this.activityClient = activityClient;
    }

    @Override
    public ActivityStatsResponse getActivityStats(StatsQueryRequest request) {
        log.info("获取活动统计数据，宠物ID：{}，周期：{}", request.getPetId(), request.getPeriod());

        // 1. 从活动服务获取数据
        List<ActivityRecordDTO> records = activityClient.getActivityRecords(
                request.getPetId(),
                request.getActivityKindId(),
                request.getStartDate(),
                request.getEndDate()
        );

        log.info("获取到 {} 条活动记录", records.size());

        // 2. 根据周期统计
        ActivityStatsResponse response = new ActivityStatsResponse();
        response.setPetId(request.getPetId());
        response.setPeriod(request.getPeriod().name());

        if (request.getPeriod() == StatsQueryRequest.StatsPeriod.MONTHLY) {
            response.setMonthlyStats(calculateMonthlyStats(records));
            calculateSummaryFromMonthly(response);
        } else {
            response.setWeeklyStats(calculateWeeklyStats(records));
            calculateSummaryFromWeekly(response);
        }

        return response;
    }

    @Override
    public ActivityStatsResponse getActivityStats(Long petId, StatsQueryRequest.StatsPeriod period) {
        StatsQueryRequest request = new StatsQueryRequest();
        request.setPetId(petId);
        request.setPeriod(period);
        request.setStartDate(LocalDateTime.now().minusMonths(3)); // 默认查最近3个月
        request.setEndDate(LocalDateTime.now());

        return getActivityStats(request);
    }

    @Override
    public ActivityStatsResponse getActivityStats(Long petId, String startDate, String endDate,
                                                  StatsQueryRequest.StatsPeriod period) {
        StatsQueryRequest request = new StatsQueryRequest();
        request.setPetId(petId);
        request.setPeriod(period);
        request.setStartDate(LocalDateTime.parse(startDate + "T00:00:00"));
        request.setEndDate(LocalDateTime.parse(endDate + "T23:59:59"));

        return getActivityStats(request);
    }

    /**
     * 按月统计数据
     */
    private List<MonthlyStatsDTO> calculateMonthlyStats(List<ActivityRecordDTO> records) {
        // 按月份分组
        Map<String, List<ActivityRecordDTO>> monthlyGroups = records.stream()
                .collect(Collectors.groupingBy(record ->
                        record.getActivityDate().format(MONTH_FORMATTER)
                ));

        List<MonthlyStatsDTO> monthlyStats = new ArrayList<>();

        for (Map.Entry<String, List<ActivityRecordDTO>> entry : monthlyGroups.entrySet()) {
            String month = entry.getKey();
            List<ActivityRecordDTO> monthRecords = entry.getValue();

            MonthlyStatsDTO monthlyStat = new MonthlyStatsDTO(month);
            monthlyStat.setTotalActivities(monthRecords.size());

            // 统计活动类型
            Map<String, Integer> activityTypeCounts = monthRecords.stream()
                    .collect(Collectors.groupingBy(
                            ActivityRecordDTO::getActivityName,
                            Collectors.summingInt(e -> 1)
                    ));
            monthlyStat.setActivityTypeCounts(activityTypeCounts);

            // 统计活动大类
            Map<String, Integer> activityKindCounts = monthRecords.stream()
                    .collect(Collectors.groupingBy(
                            ActivityRecordDTO::getActivityKindName,
                            Collectors.summingInt(e -> 1)
                    ));
            monthlyStat.setActivityKindCounts(activityKindCounts);

            monthlyStats.add(monthlyStat);
        }

        // 按月份排序
        monthlyStats.sort(Comparator.comparing(MonthlyStatsDTO::getYearMonth).reversed());

        return monthlyStats;
    }

    /**
     * 按周统计数据
     */
    private List<WeeklyStatsDTO> calculateWeeklyStats(List<ActivityRecordDTO> records) {
        // 按周分组（ISO周标准）
        Map<String, List<ActivityRecordDTO>> weeklyGroups = records.stream()
                .collect(Collectors.groupingBy(record -> {
                    LocalDateTime date = record.getActivityDate();
                    int year = date.getYear();
                    int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
                    return String.format("%d-W%02d", year, week);
                }));

        List<WeeklyStatsDTO> weeklyStats = new ArrayList<>();

        for (Map.Entry<String, List<ActivityRecordDTO>> entry : weeklyGroups.entrySet()) {
            String weekKey = entry.getKey();
            List<ActivityRecordDTO> weekRecords = entry.getValue();

            // 获取周的开始和结束日期
            LocalDateTime firstDate = weekRecords.stream()
                    .map(ActivityRecordDTO::getActivityDate)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            LocalDateTime lastDate = weekRecords.stream()
                    .map(ActivityRecordDTO::getActivityDate)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            int weekNumber = firstDate.get(WeekFields.ISO.weekOfWeekBasedYear());
            String weekRange = firstDate.format(DATE_FORMATTER) + " - " +
                    lastDate.format(DATE_FORMATTER);

            WeeklyStatsDTO weeklyStat = new WeeklyStatsDTO(weekKey, weekNumber, weekRange);
            weeklyStat.setTotalActivities(weekRecords.size());

            // 统计活动类型
            Map<String, Integer> activityTypeCounts = weekRecords.stream()
                    .collect(Collectors.groupingBy(
                            ActivityRecordDTO::getActivityName,
                            Collectors.summingInt(e -> 1)
                    ));
            weeklyStat.setActivityTypeCounts(activityTypeCounts);

            // 统计活动大类
            Map<String, Integer> activityKindCounts = weekRecords.stream()
                    .collect(Collectors.groupingBy(
                            ActivityRecordDTO::getActivityKindName,
                            Collectors.summingInt(e -> 1)
                    ));
            weeklyStat.setActivityKindCounts(activityKindCounts);

            weeklyStats.add(weeklyStat);
        }

        // 按周排序
        weeklyStats.sort(Comparator.comparing(WeeklyStatsDTO::getYearWeek).reversed());

        return weeklyStats;
    }

    /**
     * 从月统计数据计算汇总信息
     */
    private void calculateSummaryFromMonthly(ActivityStatsResponse response) {
        List<MonthlyStatsDTO> monthlyStats = response.getMonthlyStats();

        if (monthlyStats.isEmpty()) {
            return;
        }

        // 计算总次数
        int total = monthlyStats.stream()
                .mapToInt(MonthlyStatsDTO::getTotalActivities)
                .sum();
        response.setTotalActivities(total);

        // 合并所有月份的数据，计算唯一类型数量
        Set<String> allActivityTypes = new HashSet<>();
        Set<String> allActivityKinds = new HashSet<>();
        Map<String, Integer> totalActivityTypeCounts = new HashMap<>();
        Map<String, Integer> totalActivityKindCounts = new HashMap<>();

        for (MonthlyStatsDTO monthly : monthlyStats) {
            allActivityTypes.addAll(monthly.getActivityTypeCounts().keySet());
            allActivityKinds.addAll(monthly.getActivityKindCounts().keySet());

            // 累计各类型次数
            monthly.getActivityTypeCounts().forEach((type, count) ->
                    totalActivityTypeCounts.merge(type, count, Integer::sum));

            monthly.getActivityKindCounts().forEach((kind, count) ->
                    totalActivityKindCounts.merge(kind, count, Integer::sum));
        }

        response.setUniqueActivityTypes(allActivityTypes.size());
        response.setUniqueActivityKinds(allActivityKinds.size());

        // 找到最频繁的活动类型
        totalActivityTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    response.setMostFrequentActivity(entry.getKey());
                    response.setMostFrequentCount(entry.getValue());
                });

        // 找到最频繁的活动大类
        totalActivityKindCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    response.setMostFrequentKind(entry.getKey());
                    response.setMostFrequentKindCount(entry.getValue());
                });
    }

    /**
     * 从周统计数据计算汇总信息
     */
    private void calculateSummaryFromWeekly(ActivityStatsResponse response) {
        // 类似上面的实现，处理周数据
        List<WeeklyStatsDTO> weeklyStats = response.getWeeklyStats();

        if (weeklyStats.isEmpty()) {
            return;
        }

        int total = weeklyStats.stream()
                .mapToInt(WeeklyStatsDTO::getTotalActivities)
                .sum();
        response.setTotalActivities(total);

        Set<String> allActivityTypes = new HashSet<>();
        Set<String> allActivityKinds = new HashSet<>();
        Map<String, Integer> totalActivityTypeCounts = new HashMap<>();
        Map<String, Integer> totalActivityKindCounts = new HashMap<>();

        for (WeeklyStatsDTO weekly : weeklyStats) {
            allActivityTypes.addAll(weekly.getActivityTypeCounts().keySet());
            allActivityKinds.addAll(weekly.getActivityKindCounts().keySet());

            weekly.getActivityTypeCounts().forEach((type, count) ->
                    totalActivityTypeCounts.merge(type, count, Integer::sum));

            weekly.getActivityKindCounts().forEach((kind, count) ->
                    totalActivityKindCounts.merge(kind, count, Integer::sum));
        }

        response.setUniqueActivityTypes(allActivityTypes.size());
        response.setUniqueActivityKinds(allActivityKinds.size());

        totalActivityTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    response.setMostFrequentActivity(entry.getKey());
                    response.setMostFrequentCount(entry.getValue());
                });

        totalActivityKindCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    response.setMostFrequentKind(entry.getKey());
                    response.setMostFrequentKindCount(entry.getValue());
                });
    }
}