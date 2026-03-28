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

    /**
     * 配置任务调度器
     * - pool-size: 调度线程池大小
     * - await-termination-seconds: 等待任务完成的最大时间
     * - wait-for-tasks-to-complete-on-shutdown: 关闭时等待任务完成
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setErrorHandler(throwable -> {
            // 记录错误但不让调度器停止
            System.err.println("Scheduled task error: " + throwable.getMessage());
        });
        return scheduler;
    }
}
