package com.petcare.backend.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReservedActivityDTO {
    private Long activityReminderId;
    private LocalDate reminderDate;
}