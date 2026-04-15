package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Moment 实体类
 * 支持冷热数据分离存储
 * 
 * 冷存储设计：
 * - migration_status: 迁移状态 (NONE/MIGRATING)
 * - last_access_time: 最后访问时间，用于判断是否进入冷存储
 * - 迁移后删除 MySQL 记录，通过 HBase RowKey 可推算性恢复
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "moments", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id")
})
public class PetMoment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "moment_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ==================== 冷热分离字段 ====================

    /**
     * 迁移状态: NONE(无需迁移) / MIGRATING(迁移中)
     * 迁移成功后此记录会被删除
     */
    @Column(name = "migration_status", length = 20)
    private String migrationStatus = "NONE";

    /**
     * 最后访问时间，用于判断是否进入冷存储
     * 每次查询时更新
     */
    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime = LocalDateTime.now();

    // ==================== 构造函数 ====================

    /**
     * 自定义构造函数用于创建新的动态 (不包含 id 和 createdAt)
     */
    public PetMoment(Long userId, String content) {
        this.userId = userId;
        this.content = content;
    }

    // ==================== 审核流字段 ====================

    /**
     * 审核状态: PENDING(审核中) / APPROVED(已通过) / REJECTED(已拒绝)
     * 默认状态为 PENDING，外界不可见
     */
    @Column(name = "audit_status", length = 20)
    private String auditStatus = "PENDING";
}