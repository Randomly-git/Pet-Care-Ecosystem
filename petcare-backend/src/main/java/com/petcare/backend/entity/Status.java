package com.petcare.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "status")
@Data
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    @JsonProperty("statusId")
    private Long statusId;

    @Column(name = "status_name", length = 100)
    @JsonProperty("statusName")
    private String statusName;

    @Column(name = "status_value", length = 20)
    @JsonProperty("statusValue")
    private String statusValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", referencedColumnName = "pet_id")
    @JsonIgnore
    private Pet pet;
}