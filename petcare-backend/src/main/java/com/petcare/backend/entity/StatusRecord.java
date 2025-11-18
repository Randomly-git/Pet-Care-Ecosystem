package com.petcare.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "status_record")
@Data
public class StatusRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_record_id")
    private Long statusRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "status_id")
    @JsonIgnore    // 如果前端不需要完整 status 对象，只要 id，可以忽略
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", referencedColumnName = "pet_id")
    @JsonIgnore    // 如果前端不需要完整 pet 对象，只要 id，可以忽略
    private Pet pet;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status_description", length = 1000)
    private String statusDescription;
}