// dto/MediaOperationEvent.java
package com.petcare.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体操作事件消息
 * 用于异步处理 COS 操作（设置标签、恢复归档等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaOperationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 操作类型
     */
    private MediaOperationType operationType;

    /**
     * 媒体文件ID
     */
    private Long mediaId;

    /**
     * 媒体文件URL
     */
    private String fileUrl;

    /**
     * 目标存储类型 (ARCHIVE: 归档, STANDARD: 标准存储)
     */
    private String targetStorageClass;

    /**
     * 标签键
     */
    private String tagKey;

    /**
     * 标签值
     */
    private String tagValue;

    /**
     * 发起时间
     */
    private LocalDateTime timestamp;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 事件描述（用于日志）
     */
    private String description;

    /**
     * 操作类型枚举
     */
    public enum MediaOperationType {
        /**
         * 设置存储类型（转冷/转热）
         */
        SET_STORAGE_CLASS,
        /**
         * 恢复归档文件
         */
        RESTORE_ARCHIVED,
        /**
         * 删除文件（物理删除）
         */
        DELETE_FILE
    }
}
