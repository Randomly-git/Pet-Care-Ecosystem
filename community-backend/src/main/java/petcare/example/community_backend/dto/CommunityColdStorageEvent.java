package petcare.example.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区冷数据迁移事件
 * 用于 MQ 消息传递，支持级联迁移
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityColdStorageEvent implements Serializable {

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
     * 目标类型（用于级联标识）
     */
    private TargetType targetType;

    /**
     * 动态ID
     */
    private Long momentId;

    /**
     * 评论ID（可选）
     */
    private Long commentId;

    /**
     * 点赞ID（可选）
     */
    private Long likeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 关联的评论ID列表（用于级联迁移）
     */
    private List<Long> relatedCommentIds;

    /**
     * 关联的点赞ID列表（用于级联迁移）
     */
    private List<Long> relatedLikeIds;

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
         * 从冷库恢复（重新设置 last_access_time）
         */
        RESTORE_FROM_COLD,

        /**
         * 从冷库删除
         */
        DELETE_FROM_COLD
    }

    /**
     * 目标类型枚举
     */
    public enum TargetType {
        /**
         * 动态
         */
        MOMENT,

        /**
         * 评论
         */
        COMMENT,

        /**
         * 点赞
         */
        LIKE
    }

    /**
     * 创建迁移事件（单个）
     */
    public static CommunityColdStorageEvent createMigrateEvent(Long momentId, Long userId,
                                                                 List<Long> commentIds, List<Long> likeIds) {
        return CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(ColdStorageOperationType.MIGRATE_TO_COLD)
                .targetType(TargetType.MOMENT)
                .momentId(momentId)
                .userId(userId)
                .relatedCommentIds(commentIds)
                .relatedLikeIds(likeIds)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    /**
     * 创建恢复事件
     *
     * @param momentId 动态ID
     * @param userId   用户ID（用于拼装 RowKey）
     */
    public static CommunityColdStorageEvent createRestoreEvent(Long momentId, Long userId) {
        return CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(ColdStorageOperationType.RESTORE_FROM_COLD)
                .targetType(TargetType.MOMENT)
                .momentId(momentId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    /**
     * 创建删除事件
     */
    public static CommunityColdStorageEvent createDeleteEvent(Long momentId) {
        return CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(ColdStorageOperationType.DELETE_FROM_COLD)
                .targetType(TargetType.MOMENT)
                .momentId(momentId)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
}
