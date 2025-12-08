package com.petcare.backend.config;

import com.petcare.backend.entity.ActivityKind;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.User;
import com.petcare.backend.repository.ActivityKindRepository;
import com.petcare.backend.repository.ActivityRepository;
import com.petcare.backend.repository.ActivityRecordRepository;
import com.petcare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据初始化器
 * 用于在应用启动时初始化基础数据
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final ActivityKindRepository activityKindRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;

    /**
     * 初始化活动种类数据
     */
    @Bean
    @Transactional
    public CommandLineRunner initData() {
        return args -> {
            log.info("开始初始化基础数据...");

            // 初始化活动种类
            initActivityKinds();

            // 初始化基础活动
            initBasicActivities();

            log.info("基础数据初始化完成");
        };
    }

    private void initActivityKinds() {
        // 检查是否已经存在活动种类数据，如果数量不对则重新初始化
        long currentCount = activityKindRepository.count();
        if (currentCount > 0 && currentCount < 9) {
            log.info("活动种类数据不完整 (当前{}个)，重新初始化", currentCount);
            // 先删除现有的活动记录
            activityRecordRepository.deleteAll();
            log.info("已清除现有活动记录");
            // 再删除现有的活动
            activityRepository.deleteAll();
            log.info("已清除现有活动");
            // 最后删除活动种类
            activityKindRepository.deleteAll();
            log.info("已清除现有活动种类");
        } else if (currentCount >= 9) {
            log.info("活动种类数据已存在 ({}个)，跳过初始化", currentCount);
            return;
        }

        List<ActivityKind> activityKinds = Arrays.asList(
            createActivityKind("喂养"),
            createActivityKind("互动"),
            createActivityKind("清洁"),
            createActivityKind("外出"),
            createActivityKind("运动"),
            createActivityKind("医疗"),
            createActivityKind("生育"),
            createActivityKind("异常"),
            createActivityKind("其他")
        );

        activityKindRepository.saveAll(activityKinds);
        log.info("成功初始化 {} 个活动种类", activityKinds.size());
    }

    private void initBasicActivities() {
        // 获取所有用户
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("没有找到用户数据，跳过活动初始化");
            return;
        }

        // 获取活动种类
        List<ActivityKind> kinds = activityKindRepository.findAll();
        if (kinds.isEmpty()) {
            log.warn("没有找到活动种类数据，跳过活动初始化");
            return;
        }

        log.info("找到 {} 个用户和 {} 个活动种类", users.size(), kinds.size());

        // 为每个用户创建基础活动
        for (User user : users) {
            // 检查该用户是否已经有活动
            long userActivityCount = activityRepository.findByUserUserIdAndState(user.getUserId(), 1).size();
            if (userActivityCount >= kinds.size()) {
                log.info("用户 {} 已有 {} 个活动，跳过", user.getName(), userActivityCount);
                continue;
            }

            // 获取用户现有的活动种类ID
            List<Long> existingActivityKindIds = activityRepository.findByUserUserIdAndState(user.getUserId(), 1)
                .stream()
                .map(activity -> activity.getActivityKind().getActivityKindId())
                .collect(Collectors.toList());

            // 为用户创建缺失的活动
            List<Activity> newActivities = kinds.stream()
                .filter(kind -> !existingActivityKindIds.contains(kind.getActivityKindId()))
                .map(kind -> createActivity(user, kind))
                .collect(Collectors.toList());

            if (!newActivities.isEmpty()) {
                activityRepository.saveAll(newActivities);
                log.info("为用户 {} 成功初始化 {} 个基础活动", user.getName(), newActivities.size());
            }
        }
    }

    private ActivityKind createActivityKind(String name) {
        ActivityKind kind = new ActivityKind();
        kind.setActivityKindName(name);
        return kind;
    }

    private Activity createActivity(User user, ActivityKind kind) {
        Activity activity = new Activity();
        activity.setActivityName(kind.getActivityKindName());
        activity.setUser(user);
        activity.setActivityKind(kind);
        activity.setState(1); // 活跃状态
        return activity;
    }
}