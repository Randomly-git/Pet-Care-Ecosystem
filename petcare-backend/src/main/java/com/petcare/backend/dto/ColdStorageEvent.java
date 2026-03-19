package com.petcare.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 冷数据迁移事件
 * 用于MQ消息传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColdStorageEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 操作类型
     */
    private ColdStorageOperationType operationType;

    /**
     * 活动记录ID
     */
    private Long activityRecordId;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 活动日期
     */
    private LocalDateTime activityDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 事件类型枚举
     */
    public enum ColdStorageOperationType {
        /**
         * 迁移到冷库
         */
        MIGRATE_TO_COLD,
        
        /**
         * 从冷库解冻
         */
        THAW_FROM_COLD,
        
        /**
         * 删除冷库数据
         */
        DELETE_FROM_COLD
    }
}
