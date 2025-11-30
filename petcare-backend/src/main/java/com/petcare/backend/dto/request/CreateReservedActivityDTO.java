package com.petcare.backend.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReservedActivityDTO {
    private Long activityId;
    private Long petId;
    private LocalDate reminderDate;
}