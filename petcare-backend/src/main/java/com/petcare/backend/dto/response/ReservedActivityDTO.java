package com.petcare.backend.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservedActivityDTO {
    private Long activityReminderId;
    private Long activityId;
    private Long petId;
    private LocalDate reminderDate;
    private String activityName;

    public ReservedActivityDTO(Long activityReminderId, Long activityId, Long petId,
                               LocalDate reminderDate, String activityName) {
        this.activityReminderId = activityReminderId;
        this.activityId = activityId;
        this.petId = petId;
        this.reminderDate = reminderDate;
        this.activityName = activityName;
    }
}