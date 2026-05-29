package com.example.demo.breed.dto;

import lombok.Data;

@Data
public class BreedResult {
    private String identifiedBreed;
    private String description;
    private String characteristics;
    private double confidence;
    private String rawAnalysis;
}
