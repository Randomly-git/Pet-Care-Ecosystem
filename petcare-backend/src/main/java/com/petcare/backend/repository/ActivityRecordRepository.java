package com.petcare.backend.repository;

import com.petcare.backend.entity.Activity;
import com.petcare.backend.entity.ActivityRecord;
import com.petcare.backend.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
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
}