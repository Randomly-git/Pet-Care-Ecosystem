// UpdateActivityDTO.java
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class UpdateActivityDTO {
    private Long activityId;
    private String activityName;
    private Long activityKindId;
}