package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 查找关注关系是否存在
    Optional<Follow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    // 统计某个用户的粉丝数 (有多少人关注了 followedId)
    long countByFollowedId(Long followedId);

    // 统计某个用户的关注数 (followerId 关注了多少人)
    long countByFollowerId(Long followerId);

    // 删除关注关系 (用于取消关注)
    void deleteByFollowerIdAndFollowedId(Long followerId, Long followedId);
}