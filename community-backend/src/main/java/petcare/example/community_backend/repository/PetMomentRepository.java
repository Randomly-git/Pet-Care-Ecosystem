package petcare.example.community_backend.repository;

import petcare.example.community_backend.model.PetMoment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    @Modifying
    @Query("UPDATE PetMoment m SET m.lastAccessTime = :now WHERE m.id IN :ids")
    void batchUpdateLastAccessTime(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    // ==================== 状态机锁定方法 ====================

    /**
     * 原子性更新迁移状态（乐观锁）
     * 只有当当前状态等于 expectedStatus 时才更新为 newStatus
     *
     * @param id 动态ID
     * @param expectedStatus 期望的当前状态
     * @param newStatus 新状态
     * @return 更新影响的行数（1表示成功，0表示状态不匹配）
     */
    @Modifying
    @Query("UPDATE PetMoment m SET m.migrationStatus = :newStatus WHERE m.id = :id AND m.migrationStatus = :expectedStatus")
    int updateMigrationStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);

    /**
     * 查询待迁移的动态（状态为 NONE 且超过指定天数未访问）
     *
     * @param status 迁移状态
     * @param threshold 时间阈值
     * @return 待迁移的动态列表
     */
    List<PetMoment> findByMigrationStatusAndLastAccessTimeBefore(String status, LocalDateTime threshold);

    /**
     * 查询指定状态的所有动态（用于恢复迁移失败遗留的数据）
     *
     * @param status 迁移状态
     * @return 动态列表
     */
    List<PetMoment> findByMigrationStatus(String status);

    // ==================== 审核流相关方法 (一致性与幂等性保障) ====================

    /**
     * 1. 列表查询隔离：只查询“已通过 (APPROVED)”的动态（用于普通用户）
     */
    List<PetMoment> findByUserIdAndAuditStatusOrderByCreatedAtDesc(Long userId, String auditStatus);

    Page<PetMoment> findAllByAuditStatusOrderByCreatedAtDesc(String auditStatus, Pageable pageable);

    /**
     * 2. 状态机 CAS 原子更新（幂等性保障）
     * 只有当当前状态等于 expectedStatus 时，才更新为 newStatus
     */
    @Modifying
    @Query("UPDATE PetMoment m SET m.auditStatus = :newStatus WHERE m.id = :id AND m.auditStatus = :expectedStatus")
    int updateAuditStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);
}