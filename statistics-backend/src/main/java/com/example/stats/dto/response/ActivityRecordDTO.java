package com.example.stats.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityRecordDTO {
    @JsonProperty("activityRecordId")
    private Long activityRecordId;

    @JsonProperty("activityId")
    private Long activityId;

    @JsonProperty("activityName")
    private String activityName;

    @JsonProperty("activityKindId")
    private Integer activityKindId;

    @JsonProperty("activityKindName")
    private String activityKindName;

    @JsonProperty("petId")
    private Long petId;

    @JsonProperty("activityDescription")
    private String activityDescription;

    @JsonProperty("activityDate")
    private LocalDateTime activityDate;

}