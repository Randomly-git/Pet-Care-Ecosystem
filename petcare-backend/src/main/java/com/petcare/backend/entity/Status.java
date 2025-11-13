package com.petcare.backend.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "status")
@Data
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long statusId;

    @Column(name = "status_name", length = 100)
    private String statusName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", referencedColumnName = "pet_id")
    private Pet pet;

    @Column(name = "state")
    private Integer state = 1; // 1表示有效，0表示已删除
}