// CreateActivityDTO.java
package com.petcare.backend.dto.response;

import lombok.Data;

@Data
public class CreateActivityDTO {
    private String activityName;
    private Long activityKindId;
    private Long petId;
}