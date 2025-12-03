package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);

    // 用于 MomentService 统计动态和评论的点赞数
    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    // 用于事务性删除
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);
}