package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.PetMoment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PetMomentRepository extends JpaRepository<PetMoment, Long> {

    /**
     * 查找属于特定用户ID的所有动态，并按创建时间倒序排列。
     */
    List<PetMoment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 获取所有用户的动态，按创建时间倒序排列，支持分页。
     */
    Page<PetMoment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据动态ID获取作者用户ID
     */
    @Query("SELECT m.userId FROM PetMoment m WHERE m.id = :momentId")
    Optional<Long> findUserIdById(@Param("momentId") Long momentId);

    // ==================== 冷热分离查询方法 ====================

    /**
     * 查询需要迁移的记录（超过指定天数未访问且未开始迁移）
     */
    @Query("SELECT m FROM PetMoment m WHERE m.lastAccessTime < :threshold " +
           "AND (m.migrationStatus IS NULL OR m.migrationStatus = 'NONE')")
    Page<PetMoment> findRecordsToMigrate(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    /**
     * 查询迁移中的记录
     */
    @Query("SELECT m FROM PetMoment m WHERE m.migrationStatus = 'MIGRATING'")
    Page<PetMoment> findMigratingRecords(Pageable pageable);

    /**
     * 统计待迁移记录数
     */
    @Query("SELECT COUNT(m) FROM PetMoment m WHERE m.migrationStatus = 'NONE' " +
           "AND m.lastAccessTime < :threshold")
    long countPendingMigrationRecords(@Param("threshold") LocalDateTime threshold);

    /**
     * 统计迁移中记录数
     */
    @Query("SELECT COUNT(m) FROM PetMoment m WHERE m.migrationStatus = 'MIGRATING'")
    long countMigratingRecords();

    /**
     * 批量更新最后访问时间
     */
    @Query("UPDATE PetMoment m SET m.lastAccessTime = :now WHERE m.id IN :ids")
    void batchUpdateLastAccessTime(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}