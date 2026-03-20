package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 点赞实体
 * 支持冷热数据分离存储
 * 
 * 冷存储设计：
 * - migration_status: 迁移状态 (NONE/MIGRATING)
 * - 迁移后删除 MySQL 记录，通过 HBase RowKey 可推算性恢复
 * - likes 关联目标可能是 MOMENT 或 COMMENT，需要级联迁移
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
})
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

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