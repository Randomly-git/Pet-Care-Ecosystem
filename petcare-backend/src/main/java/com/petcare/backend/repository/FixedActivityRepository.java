package com.petcare.backend.repository;

import com.petcare.backend.dto.response.FixedActivityDTO;
import com.petcare.backend.entity.FixedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedActivityRepository extends JpaRepository<FixedActivity, Long> {

    List<FixedActivity> findByPetId(Long petId);

    List<FixedActivity> findByActivityId(Long activityId);

    @Query("SELECT fa FROM FixedActivity fa WHERE fa.petId = :petId AND fa.activityId = :activityId")
    Optional<FixedActivity> findByPetIdAndActivityId(@Param("petId") Long petId, @Param("activityId") Long activityId);

    boolean existsByActivityId(Long activityId);

    // 修改查询方法，添加 reminder_date 字段
    @Query(value = "SELECT fa.fixed_activity_id, fa.activity_id, fa.pet_id, fa.gap_time, " +
            "a.activity_name, ak.activity_kind_id, ak.activity_kind_name, p.name, " +
            "ar.reminder_date " +  // 新增 reminder_date 字段
            "FROM fixed_activity fa " +
            "JOIN activity a ON fa.activity_id = a.activity_id " +
            "JOIN activity_kind ak ON a.activity_kind_id = ak.activity_kind_id " +
            "JOIN pets p ON fa.pet_id = p.pet_id " +
            "LEFT JOIN activity_reminder ar ON fa.activity_id = ar.activity_id AND ar.type = 1 " +  // 关联定时活动提醒
            "WHERE fa.pet_id = :petId",
            nativeQuery = true)
    List<Object[]> findFixedActivitiesWithDetailsByPetId(@Param("petId") Long petId);
}