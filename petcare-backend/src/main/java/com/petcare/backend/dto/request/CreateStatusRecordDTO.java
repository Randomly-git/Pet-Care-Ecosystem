// CreateStatusRecordDTO.java
package com.petcare.backend.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateStatusRecordDTO {
    private Long petId;
    private Long statusId;
    private LocalDate startDate;
    private String statusDescription;
}