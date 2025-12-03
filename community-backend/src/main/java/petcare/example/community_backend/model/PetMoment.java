package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Moment 实体类
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

    // 绑定到用户微服务的用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob // 对应 TEXT 类型
    @Column(nullable = false)
    private String content;

    // 动态创建时间，默认当前时间，且创建后不可更新
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 自定义构造函数用于创建新的动态 (不包含 id 和 createdAt)
     */
    public PetMoment(Long userId, String content) {
        this.userId = userId;
        this.content = content;
    }
}