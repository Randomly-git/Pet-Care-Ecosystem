package petcare.example.community_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "follows", uniqueConstraints = {
        // 确保同一用户只能关注另一用户一次
        @UniqueConstraint(columnNames = {"follower_id", "followed_id"})
})
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId; // 关注者ID

    @Column(name = "followed_id", nullable = false)
    private Long followedId; // 被关注者ID (作者ID)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}