package com.petcare.backend.dto.request;

import lombok.Data;

@Data
public class UpdateFixedActivityDTO {
    private Long fixedActivityId;
    private Integer gapTime;
}