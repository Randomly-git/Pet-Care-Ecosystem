package com.petcare.backend.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "activity")
@Data
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "activity_name", length = 100)
    private String activityName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_kind_id", referencedColumnName = "activity_kind_id")
    private ActivityKind activityKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", referencedColumnName = "pet_id")
    private Pet pet;

    @Column(name = "state")
    private Integer state = 1; // 1表示有效，0表示已删除
}