package com.petcare.backend.dto.request;

import lombok.Data;

@Data
public class CreateFixedActivityDTO {
    private Long petId;
    private Long activityId;
    private Integer gapTime;
}