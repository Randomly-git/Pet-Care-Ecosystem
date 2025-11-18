// StatusRecordDTO.java
package com.petcare.backend.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StatusRecordDTO {
    private Long statusRecordId;
    private Long statusId;
    private String statusName;
    private Long petId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String statusDescription;

    public StatusRecordDTO(Long statusRecordId, Long statusId, String statusName,
                           Long petId, LocalDate startDate, LocalDate endDate,
                           String statusDescription) {
        this.statusRecordId = statusRecordId;
        this.statusId = statusId;
        this.statusName = statusName;
        this.petId = petId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.statusDescription = statusDescription;
    }
    public StatusRecordDTO(){}
}