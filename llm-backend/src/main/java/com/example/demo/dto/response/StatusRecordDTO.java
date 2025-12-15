package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public class StatusRecordDTO {
    @JsonProperty("statusRecordId")
    private Long statusRecordId;

    @JsonProperty("statusId")
    private Long statusId;

    @JsonProperty("statusName")
    private String statusName;

    @JsonProperty("petId")
    private Long petId;

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("statusDescription")
    private String statusDescription;

    @JsonProperty("mediaFiles")
    private List<MediaFileDTO> mediaFiles;

    @JsonProperty("mediaCount")
    private Integer mediaCount;

    @JsonProperty("firstMediaUrl")
    private String firstMediaUrl;

    // Getters and Setters
    public Long getStatusRecordId() { return statusRecordId; }
    public void setStatusRecordId(Long statusRecordId) { this.statusRecordId = statusRecordId; }

    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

    public List<MediaFileDTO> getMediaFiles() { return mediaFiles; }
    public void setMediaFiles(List<MediaFileDTO> mediaFiles) { this.mediaFiles = mediaFiles; }

    public Integer getMediaCount() { return mediaCount; }
    public void setMediaCount(Integer mediaCount) { this.mediaCount = mediaCount; }

    public String getFirstMediaUrl() { return firstMediaUrl; }
    public void setFirstMediaUrl(String firstMediaUrl) { this.firstMediaUrl = firstMediaUrl; }
}

class MediaFileDTO {
    @JsonProperty("mediaId")
    private Long mediaId;

    @JsonProperty("fileName")
    private String fileName;

    // ... 其他字段的getter/setter
}
