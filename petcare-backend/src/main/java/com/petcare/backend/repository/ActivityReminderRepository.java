package com.petcare.backend.repository;

import com.petcare.backend.entity.ActivityReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActivityReminderRepository extends JpaRepository<ActivityReminder, Long> {

    // 基于字段的查询
    List<ActivityReminder> findByActivityIdAndTypeAndPetId(Long activityId, Integer type, Long petId);

    // 在 ActivityReminderRepository 中添加以下方法
    @Query(value = "SELECT ar.activity_reminder_id, ar.activity_id, ar.pet_id, ar.reminder_date, a.activity_name " +
            "FROM activity_reminder ar " +
            "JOIN activity a ON ar.activity_id = a.activity_id " +
            "WHERE ar.pet_id = :petId AND ar.type = 2 " +
            "ORDER BY ar.reminder_date ASC",
            nativeQuery = true)
    List<Object[]> findReservedActivitiesWithDetailsByPetId(@Param("petId") Long petId);

    // 在 ActivityReminderRepository 中添加以下方法
    @Query(value = "SELECT ar.activity_reminder_id, ar.activity_id, ar.pet_id, ar.reminder_date, ar.type, a.activity_name " +
            "FROM activity_reminder ar " +
            "JOIN activity a ON ar.activity_id = a.activity_id " +
            "WHERE ar.pet_id = :petId AND ar.reminder_date <= CURRENT_DATE " +
            "ORDER BY ar.reminder_date ASC",
            nativeQuery = true)
    List<Object[]> findOverdueRemindersByPetId(@Param("petId") Long petId);
}