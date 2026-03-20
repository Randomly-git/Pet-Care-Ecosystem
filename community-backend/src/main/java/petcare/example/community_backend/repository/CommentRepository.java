package petcare.example.community_backend.repository;

import org.springframework.data.jpa.repository.Query;
import petcare.example.community_backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId);

    // 用于 MomentService 统计动态总评论数
    long countByMomentId(Long momentId);

    @Modifying
    void deleteByMomentId(Long momentId);

    // 批量统计某批动态下的评论数
    @Query("SELECT c.momentId, COUNT(c) FROM Comment c WHERE c.momentId IN :momentIds GROUP BY c.momentId")
    List<Object[]> countByMomentIdIn(Collection<Long> momentIds);

    // ==================== 冷热分离查询方法 ====================

    /**
     * 按动态ID列表查询评论
     */
    List<Comment> findByMomentIdIn(Collection<Long> momentIds);

    /**
     * 统计需要迁移的评论数
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.momentId = :momentId " +
           "AND (c.migrationStatus IS NULL OR c.migrationStatus = 'NONE')")
    long countPendingMigrationByMomentId(@Param("momentId") Long momentId);

    /**
     * 批量更新评论迁移状态
     */
    @Modifying
    @Query("UPDATE Comment c SET c.migrationStatus = :status WHERE c.momentId = :momentId")
    void updateMigrationStatusByMomentId(@Param("momentId") Long momentId, @Param("status") String status);
}