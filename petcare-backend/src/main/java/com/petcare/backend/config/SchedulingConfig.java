package com.petcare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度器配置
 * 
 * 问题背景：
 * Spring 默认的调度器是单线程的，如果：
 * 1. 定时任务执行时间超过间隔时间
 * 2. 或者任务执行时发生异常
 * 都会导致后续任务被延迟或跳过
 * 
 * 本配置：
 * 1. 启用多线程调度
 * 2. 配置合理的线程池大小
 * 3. 确保任务独立执行
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    // @EnableScheduling 让所有 @Scheduled 方法生效；没有它，冷数据迁移不会自动运行。
    // @EnableAsync 允许异步任务使用独立线程执行，避免阻塞 Web 请求线程。

    /**
     * 配置任务调度器
     * - pool-size: 调度线程池大小
     * - await-termination-seconds: 等待任务完成的最大时间
     * - wait-for-tasks-to-complete-on-shutdown: 关闭时等待任务完成
     */
    @Bean
    public TaskScheduler taskScheduler() {
        // 创建 Spring 使用的调度器，而不是使用默认的单线程调度器。
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 五个线程允许多个定时任务并行，但仍限制并发量，避免同时压垮数据库和 HBase。
        scheduler.setPoolSize(5);
        // 统一线程名，便于日志和线程转储定位具体调度任务。
        scheduler.setThreadNamePrefix("scheduled-task-");
        // 应用关闭时最多等待 60 秒，让正在进行的迁移有机会完成。
        scheduler.setAwaitTerminationSeconds(60);
        // 关闭时等待任务完成，减少迁移到一半留下 MIGRATING 状态的概率。
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setErrorHandler(throwable -> {
            // 捕获调度异常，避免单次任务异常导致调度线程停止执行后续任务。
            // 记录错误但不让调度器停止
            System.err.println("Scheduled task error: " + throwable.getMessage());
        });
        return scheduler;
    }
}
