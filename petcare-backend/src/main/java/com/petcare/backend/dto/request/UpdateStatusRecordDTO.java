// UpdateStatusRecordDTO.java
package com.petcare.backend.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateStatusRecordDTO {
    private Long statusRecordId;
    private Long statusId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String statusDescription;
}