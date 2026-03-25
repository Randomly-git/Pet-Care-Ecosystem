package petcare.example.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.model.TargetType;

import java.time.LocalDateTime;

/**
 * HBase 扁平化归档记录
 *
 * 对应 HBase 表：community_moments_archive
 * RowKey 格式: {userId前4位}_{momentId}
 *
 * 存储结构：
 * d:moment_id      -> 动态ID
 * d:user_id        -> 作者用户ID
 * d:content        -> 动态内容
 * d:created_at     -> 创建时间
 * d:full_data      -> GZIP压缩的JSON（二进制）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HBaseArchiveRecord {

    private Long momentId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private byte[] fullData;  // GZIP 压缩的 JSON

    /**
     * 将归档记录转换为 PetMoment 实体（用于恢复）
     */
    public PetMoment toPetMoment() {
        PetMoment moment = new PetMoment();
        moment.setId(this.momentId);
        moment.setUserId(this.userId);
        moment.setContent(this.content);
        moment.setCreatedAt(this.createdAt);
        moment.setLastAccessTime(LocalDateTime.now());
        moment.setMigrationStatus("NONE");
        return moment;
    }

    /**
     * 从 PetMoment 构建归档记录
     */
    public static HBaseArchiveRecord fromMoment(PetMoment moment) {
        return HBaseArchiveRecord.builder()
                .momentId(moment.getId())
                .userId(moment.getUserId())
                .content(moment.getContent())
                .createdAt(moment.getCreatedAt())
                .build();
    }

    /**
     * 从归档数据中的 ArchivedComment 转换为 Comment 实体
     */
    public static Comment toCommentEntity(ColdArchiveData.ArchivedComment archivedComment) {
        Comment comment = new Comment();
        comment.setId(archivedComment.getCommentId());
        comment.setMomentId(archivedComment.getMomentId());
        comment.setUserId(archivedComment.getUserId());
        comment.setContent(archivedComment.getContent());
        comment.setParentId(archivedComment.getParentId());
        comment.setCreatedAt(archivedComment.getCreatedAt());
        comment.setMigrationStatus("NONE");
        return comment;
    }

    /**
     * 从归档数据中的 ArchivedLike 转换为 Like 实体
     */
    public static Like toLikeEntity(ColdArchiveData.ArchivedLike archivedLike) {
        Like like = new Like();
        like.setId(archivedLike.getLikeId());
        like.setUserId(archivedLike.getUserId());
        like.setTargetType(TargetType.valueOf(archivedLike.getTargetType().name()));
        like.setTargetId(archivedLike.getTargetId());
        like.setCreatedAt(archivedLike.getCreatedAt());
        like.setMigrationStatus("NONE");
        return like;
    }

    /**
     * 将 Comment 实体转换为 ArchivedComment
     */
    public static ColdArchiveData.ArchivedComment fromCommentEntity(Comment comment, String userName, String userAvatar) {
        return ColdArchiveData.ArchivedComment.builder()
                .commentId(comment.getId())
                .momentId(comment.getMomentId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .userName(userName)
                .userAvatar(userAvatar)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * 将 Like 实体转换为 ArchivedLike
     */
    public static ColdArchiveData.ArchivedLike fromLikeEntity(Like like, String userName, String userAvatar) {
        ColdArchiveData.ArchivedLikeTargetType archivedTargetType;
        if (like.getTargetType() == TargetType.MOMENT) {
            archivedTargetType = ColdArchiveData.ArchivedLikeTargetType.MOMENT;
        } else {
            archivedTargetType = ColdArchiveData.ArchivedLikeTargetType.COMMENT;
        }

        return ColdArchiveData.ArchivedLike.builder()
                .likeId(like.getId())
                .userId(like.getUserId())
                .targetType(archivedTargetType)
                .targetId(like.getTargetId())
                .userName(userName)
                .userAvatar(userAvatar)
                .createdAt(like.getCreatedAt())
                .build();
    }
}
