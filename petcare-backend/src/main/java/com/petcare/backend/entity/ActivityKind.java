package com.petcare.backend.entity;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "activity_kind")
@Data
public class ActivityKind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_kind_id")
    private Long activityKindId;

    @Column(name = "activity_kind_name", length = 100)
    private String activityKindName;
}