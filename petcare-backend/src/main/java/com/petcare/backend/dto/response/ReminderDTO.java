package com.petcare.backend.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReminderDTO {
    private Long activityReminderId;
    private Long activityId;
    private Long petId;
    private LocalDate reminderDate;
    private Integer type;
    private String activityName;

    public ReminderDTO(Long activityReminderId, Long activityId, Long petId,
                       LocalDate reminderDate, Integer type, String activityName) {
        this.activityReminderId = activityReminderId;
        this.activityId = activityId;
        this.petId = petId;
        this.reminderDate = reminderDate;
        this.type = type;
        this.activityName = activityName;
    }
}