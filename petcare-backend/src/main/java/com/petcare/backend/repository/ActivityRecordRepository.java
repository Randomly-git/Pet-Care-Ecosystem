package com.petcare.backend.repository;

import com.petcare.backend.dto.response.ActivityRecordDTO;
import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {

    // 根据宠物ID查找活动记录
    List<ActivityRecord> findByPetPetId(Long petId);

    // 根据活动ID查找活动记录
    List<ActivityRecord> findByActivityActivityId(Long activityId);

    // 根据活动日期范围查找
    List<ActivityRecord> findByActivityDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // 根据宠物ID和活动日期范围查找
    List<ActivityRecord> findByPetPetIdAndActivityDateBetween(Long petId, LocalDateTime startDate, LocalDateTime endDate);

    // 查找特定宠物的最新活动记录
    @Query("SELECT ar FROM ActivityRecord ar WHERE ar.pet.petId = :petId ORDER BY ar.activityDate DESC")
    List<ActivityRecord> findRecentActivityRecordsByPetId(@Param("petId") Long petId);

    // 统计宠物在某个时间范围内的活动数量
    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.pet.petId = :petId AND ar.activityDate BETWEEN :startDate AND :endDate")
    Long countActivityRecordsByPetIdAndDateRange(@Param("petId") Long petId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // 根据宠物和活动查找记录
    List<ActivityRecord> findByPetAndActivity(Pet pet, Activity activity);

    // 查找包含特定描述的活动记录
    List<ActivityRecord> findByActivityDescriptionContaining(String keyword);

    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.pet.petId = :petId")
    Long countByPetPetId(@Param("petId") Long petId);

    @Query(value = "SELECT new com.petcare.backend.dto.response.ActivityRecordDTO(" +
            "ar.activityRecordId, a.activityId, a.activityName, " +
            "ak.activityKindId, ak.activityKindName, p.petId, " +
            "ar.activityDescription, ar.activityDate) " +
            "FROM ActivityRecord ar " +
            "JOIN ar.activity a " +
            "JOIN a.activityKind ak " +
            "JOIN ar.pet p " +
            "WHERE p.petId = :petId " +
            "AND (:startDate IS NULL OR ar.activityDate >= :startDate) " +
            "AND (:endDate IS NULL OR ar.activityDate <= :endDate) " +
            "AND (:activityKindId IS NULL OR ak.activityKindId = :activityKindId)",
            countQuery = "SELECT count(ar) FROM ActivityRecord ar " +
                    "JOIN ar.activity a JOIN a.activityKind ak JOIN ar.pet p " +
                    "WHERE p.petId = :petId " +
                    "AND (:startDate IS NULL OR ar.activityDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ar.activityDate <= :endDate) " +
                    "AND (:activityKindId IS NULL OR ak.activityKindId = :activityKindId)")
    Page<ActivityRecordDTO> findActivityRecordsWithDetails(@Param("petId") Long petId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate,
                                                          @Param("activityKindId") Long activityKindId,
                                                          Pageable pageable);

    // 根据宠物ID列表批量查找活动记录
    List<ActivityRecord> findByPetPetIdIn(List<Long> petIds);

    // ==================== 冷热分离查询方法（简化后） ====================

    /**
     * 查询需要迁移的记录（超过指定天数且未开始迁移）
     * 根据 activity_date 判断，不需要 last_access_time
     */
    @Query("SELECT ar FROM ActivityRecord ar WHERE ar.activityDate < :threshold " +
           "AND (ar.migrationStatus IS NULL OR ar.migrationStatus = 'NONE')")
    Page<ActivityRecord> findRecordsToMigrate(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    /**
     * 查询迁移中的记录
     */
    @Query("SELECT ar FROM ActivityRecord ar WHERE ar.migrationStatus = 'MIGRATING'")
    List<ActivityRecord> findMigratingRecords(Pageable pageable);

    /**
     * 查询超过指定天数的记录数量
     */
    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.activityDate < :threshold " +
           "AND (ar.migrationStatus IS NULL OR ar.migrationStatus = 'NONE')")
    long countOldRecordsNeedingMigration(@Param("threshold") LocalDateTime threshold);

    // ==================== 统计查询方法（简化后） ====================

    /**
     * 统计总记录数
     */
    @Query("SELECT COUNT(ar) FROM ActivityRecord ar")
    long countTotalRecords();

    /**
     * 统计待迁移记录数
     */
    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.migrationStatus = 'NONE' " +
           "AND ar.activityDate < :threshold")
    long countPendingMigrationRecords(@Param("threshold") LocalDateTime threshold);

    /**
     * 统计迁移中记录数
     */
    @Query("SELECT COUNT(ar) FROM ActivityRecord ar WHERE ar.migrationStatus = 'MIGRATING'")
    long countMigratingRecords();

    /**
     * 乐观锁：原子性更新迁移状态
     * 只有当记录的当前状态等于 expectedStatus 时，才更新为 newStatus
     *
     * @param recordId      记录ID
     * @param expectedStatus 期望的当前状态
     * @param newStatus     新状态
     * @return 更新成功的记录数（0表示状态不匹配，1表示更新成功）
     */
    @Modifying
    @Query("UPDATE ActivityRecord ar SET ar.migrationStatus = :newStatus WHERE ar.activityRecordId = :recordId AND ar.migrationStatus = :expectedStatus")
    int updateMigrationStatus(@Param("recordId") Long recordId,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("newStatus") String newStatus);

    List<ActivityRecord> findByPet_PetIdAndBertResultBetween(Long petId, int i, int i1);
}
