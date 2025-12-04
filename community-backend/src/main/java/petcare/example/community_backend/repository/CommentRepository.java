package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId);

    // 用于 MomentService 统计动态总评论数
    long countByMomentId(Long momentId);

    @Modifying
    void deleteByMomentId(Long momentId);
}