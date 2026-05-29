package com.example.demo.breed.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cat_breed")
@Data
public class CatBreed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String breedName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String characteristics;
}
