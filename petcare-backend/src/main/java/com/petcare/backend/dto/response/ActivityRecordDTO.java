package com.petcare.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityRecordDTO {
    private Long activityRecordId;
    private Long activityId;
    private String activityName;
    private Long activityKindId;
    private String activityKindName;
    private Long petId;
    private String activityDescription;
    private LocalDateTime activityDate;

    // 构造函数
    public ActivityRecordDTO(Long activityRecordId, Long activityId, String activityName,
                             Long activityKindId, String activityKindName, Long petId,
                             String activityDescription, LocalDateTime activityDate) {
        this.activityRecordId = activityRecordId;
        this.activityId = activityId;
        this.activityName = activityName;
        this.activityKindId = activityKindId;
        this.activityKindName = activityKindName;
        this.petId = petId;
        this.activityDescription = activityDescription;
        this.activityDate = activityDate;
    }
}