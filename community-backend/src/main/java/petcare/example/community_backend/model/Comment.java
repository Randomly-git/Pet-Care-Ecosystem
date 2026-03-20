package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 评论实体
 * 支持冷热数据分离存储
 * 
 * 冷存储设计：
 * - migration_status: 迁移状态 (NONE/MIGRATING)
 * - 迁移后删除 MySQL 记录，通过 HBase RowKey 可推算性恢复
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "comments", indexes = {
        @Index(name = "idx_moment_id", columnList = "moment_id"),
        @Index(name = "idx_user_moment", columnList = "user_id, moment_id")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "moment_id", nullable = false)
    private Long momentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ==================== 冷热分离字段 ====================

    /**
     * 迁移状态: NONE(无需迁移) / MIGRATING(迁移中)
     * 迁移成功后此记录会被删除
     */
    @Column(name = "migration_status", length = 20)
    private String migrationStatus = "NONE";
}