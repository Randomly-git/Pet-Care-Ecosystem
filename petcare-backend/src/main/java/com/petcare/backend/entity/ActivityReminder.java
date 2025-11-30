package com.petcare.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "activity_reminder")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_reminder_id")
    private Long activityReminderId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;

    @Column(name = "type", nullable = false)
    private Integer type; // 1: 定时活动, 2: 一次性提醒

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    // 移除所有关联关系
}