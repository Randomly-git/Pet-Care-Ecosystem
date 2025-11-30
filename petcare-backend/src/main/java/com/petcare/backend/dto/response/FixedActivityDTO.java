package com.petcare.backend.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FixedActivityDTO {
    private Long fixedActivityId;
    private Long activityId;
    private String activityName;
    private Long activityKindId;
    private String activityKindName;
    private Long petId;
    private String petName;
    private Integer gapTime;
    private LocalDate nextReminderDate; // 下一次提醒日期
}