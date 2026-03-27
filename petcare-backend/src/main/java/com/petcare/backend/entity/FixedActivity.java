package com.petcare.backend.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "fixed_activity")
@Data
public class FixedActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixed_activity_id")
    private Long fixedActivityId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "gap_time", nullable = false)
    private Integer gapTime;

    @Column(name = "pet_id", nullable = false)
    private Long petId;
}
