package petcare.example.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扁平化冷归档数据模型
 *
 * 设计思想：将动态、评论、点赞合并存储到单个 HBase RowKey 中
 * 恢复时只需一次 Get 操作即可获取完整数据
 *
 * JSON 结构：
 * {
 *   "comments": [...],
 *   "likes": [...],
 *   "metadata": {
 *     "commentCount": 10,
 *     "likeCount": 50,
 *     "snapshotTime": "2026-03-25T00:00:00"
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColdArchiveData {

    /**
     * 评论列表
     */
    private List<ArchivedComment> comments;

    /**
     * 点赞列表
     */
    private List<ArchivedLike> likes;

    /**
     * 元数据
     */
    private ArchiveMetadata metadata;

    /**
     * 归档元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveMetadata {
        private int commentCount;
        private int likeCount;
        private LocalDateTime snapshotTime;
    }

    /**
     * 归档的评论数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchivedComment {
        private Long commentId;
        private Long momentId;
        private Long userId;
        private String content;
        private Long parentId;
        private String userName;
        private String userAvatar;
        private LocalDateTime createdAt;
    }

    /**
     * 归档的点赞数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchivedLike {
        private Long likeId;
        private Long userId;
        private ArchivedLikeTargetType targetType;
        private Long targetId;
        private String userName;
        private String userAvatar;
        private LocalDateTime createdAt;
    }

    /**
     * 点赞目标类型枚举
     */
    public enum ArchivedLikeTargetType {
        MOMENT, COMMENT
    }
}
