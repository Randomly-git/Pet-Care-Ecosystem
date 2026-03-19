package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体
 * 存储用户的通知记录（点赞、评论、关注等）
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_read", columnList = "userId,isRead")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知类型: LIKE, COMMENT, FOLLOW, REPLY
     */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * 接收通知的用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 触发操作的用户ID
     */
    @Column(nullable = false)
    private Long actorUserId;

    /**
     * 触发操作的用户名称
     */
    @Column(length = 100)
    private String actorUserName;

    /**
     * 触发操作的用户头像
     */
    @Column(length = 500)
    private String actorUserAvatar;

    /**
     * 关联的业务ID（如动态ID、评论ID等）
     */
    @Column
    private Long businessId;

    /**
     * 业务类型: MOMENT, COMMENT, USER
     */
    @Column(length = 20)
    private String businessType;

    /**
     * 通知内容摘要
     */
    @Column(length = 500)
    private String content;

    /**
     * 是否已读
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 阅读时间
     */
    private LocalDateTime readAt;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        LIKE,    // 点赞通知
        COMMENT, // 评论通知
        FOLLOW,  // 关注通知
        REPLY    // 回复通知
    }
}
