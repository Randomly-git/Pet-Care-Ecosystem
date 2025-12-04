package com.petcare.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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


    // 新增媒体相关字段
    private List<MediaResponse> mediaFiles;  // 媒体文件列表
    private Integer mediaCount;              // 媒体文件数量
    private String firstMediaUrl;           // 第一个媒体文件的URL（用于列表展示）

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

    // getter/setter 方法
    public void setMediaFiles(List<MediaResponse> mediaFiles) {
        this.mediaFiles = mediaFiles;
        this.mediaCount = mediaFiles != null ? mediaFiles.size() : 0;
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            this.firstMediaUrl = mediaFiles.getFirst().getFileUrl();
        }
    }

    public ActivityRecordDTO(){}
}