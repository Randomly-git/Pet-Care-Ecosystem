// StatusRecordDTO.java
package com.petcare.backend.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class StatusRecordDTO {
    private Long statusRecordId;
    private Long statusId;
    private String statusName;
    private Long petId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String statusDescription;

    // 新增媒体相关字段
    private List<MediaResponse> mediaFiles;  // 媒体文件列表
    private Integer mediaCount;              // 媒体文件数量
    private String firstMediaUrl;           // 第一个媒体文件的URL（用于列表展示）

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

    // getter/setter 方法
    public void setMediaFiles(List<MediaResponse> mediaFiles) {
        this.mediaFiles = mediaFiles;
        this.mediaCount = mediaFiles != null ? mediaFiles.size() : 0;
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            this.firstMediaUrl = mediaFiles.get(0).getFileUrl();
        }
    }
}