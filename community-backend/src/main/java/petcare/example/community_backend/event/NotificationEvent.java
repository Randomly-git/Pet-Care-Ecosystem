package petcare.example.community_backend.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知事件模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 通知类型
     */
    private NotificationType type;

    /**
     * 触发操作的用户ID
     */
    private Long actorUserId;

    /**
     * 触发操作的用户名称
     */
    private String actorUserName;

    /**
     * 接收通知的用户ID
     */
    private Long targetUserId;

    /**
     * 关联的业务ID（如动态ID、评论ID等）
     */
    private Long businessId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 通知内容（可选，用于快速展示）
     */
    private String content;

    /**
     * 事件发生时间
     */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        LIKE,        // 点赞通知
        COMMENT,     // 评论通知
        FOLLOW,      // 关注通知
        REPLY        // 回复通知
    }

    /**
     * 创建点赞事件
     */
    public static NotificationEvent createLikeEvent(Long actorUserId, String actorUserName,
                                                     Long targetUserId, Long momentId) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .type(NotificationType.LIKE)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .targetUserId(targetUserId)
                .businessId(momentId)
                .businessType("MOMENT")
                .content(actorUserName + "赞了你的动态")
                .eventTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建评论事件
     */
    public static NotificationEvent createCommentEvent(Long actorUserId, String actorUserName,
                                                        Long targetUserId, Long commentId, Long momentId) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .type(NotificationType.COMMENT)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .targetUserId(targetUserId)
                .businessId(commentId)
                .businessType("COMMENT")
                .content(actorUserName + "评论了你的动态")
                .eventTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建关注事件
     */
    public static NotificationEvent createFollowEvent(Long actorUserId, String actorUserName,
                                                       Long targetUserId) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .type(NotificationType.FOLLOW)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .targetUserId(targetUserId)
                .businessId(null)
                .businessType("USER")
                .content(actorUserName + "关注了你")
                .eventTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建回复事件
     */
    public static NotificationEvent createReplyEvent(Long actorUserId, String actorUserName,
                                                      Long targetUserId, Long replyId, Long commentId) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .type(NotificationType.REPLY)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .targetUserId(targetUserId)
                .businessId(replyId)
                .businessType("REPLY")
                .content(actorUserName + "回复了你的评论")
                .eventTime(LocalDateTime.now())
                .build();
    }
}
