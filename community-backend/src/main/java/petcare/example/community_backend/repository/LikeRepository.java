package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.TargetType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);

    // 用于 MomentService 统计动态和评论的点赞数
    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    // 用于事务性删除
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);

    @Modifying
    void deleteByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    // 批量统计点赞数，用于 CommentService/MomentService 数据聚合
    List<Object[]> countByTargetTypeAndTargetIdIn(TargetType targetType, Collection<Long> targetIds);

    /**
     * 批量统计点赞数。返回目标 ID 和点赞数。
     * 对应 SQL: SELECT target_id, COUNT(like_id) FROM likes WHERE target_type = :targetType AND target_id IN (:targetIds) GROUP BY target_id
     */
    @Query("SELECT l.targetId, COUNT(l) FROM Like l WHERE l.targetType = :targetType AND l.targetId IN :targetIds GROUP BY l.targetId")
    List<Object[]> countLikesByTargetTypeAndTargetIdIn(TargetType targetType, Collection<Long> targetIds);

    // 批量删除点赞记录，用于级联删除
    @Modifying
    void deleteByTargetTypeAndTargetIdIn(TargetType targetType, Collection<Long> targetIds);

    // ==================== 冷热分离查询方法 ====================

    /**
     * 按目标类型和目标ID查询点赞
     */
    List<Like> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    /**
     * 按目标ID列表查询点赞
     */
    List<Like> findByTargetTypeAndTargetIdIn(TargetType targetType, Collection<Long> targetIds);

    /**
     * 统计需要迁移的点赞数
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.targetType = :targetType AND l.targetId = :targetId " +
           "AND (l.migrationStatus IS NULL OR l.migrationStatus = 'NONE')")
    long countPendingMigrationByTarget(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId);

    /**
     * 批量更新点赞迁移状态
     */
    @Modifying
    @Query("UPDATE Like l SET l.migrationStatus = :status WHERE l.targetType = :targetType AND l.targetId = :targetId")
    void updateMigrationStatusByTarget(@Param("targetType") TargetType targetType,
                                      @Param("targetId") Long targetId,
                                      @Param("status") String status);
}