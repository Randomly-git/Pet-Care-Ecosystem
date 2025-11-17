// dto/MediaResponse.java
package com.petcare.media.dto;

import com.petcare.media.entity.RelatedType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MediaResponse {
    private Long mediaId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadTime;
    private Long petId;
    private String relatedType;  // 返回字符串给前端
    private String relatedTypeDesc; // 类型描述
    private Long relatedId;

    public static MediaResponse fromEntity(com.petcare.media.entity.MediaFile mediaFile) {
        MediaResponse response = new MediaResponse();
        response.setMediaId(mediaFile.getId());
        response.setFileName(mediaFile.getFileName());
        response.setFileUrl(mediaFile.getFileUrl());
        response.setFileType(mediaFile.getFileType());
        response.setFileSize(mediaFile.getFileSize());
        response.setUploadTime(mediaFile.getUploadTime());
        response.setPetId(mediaFile.getPetId());
        response.setRelatedType(mediaFile.getRelatedType().name());
        response.setRelatedTypeDesc(mediaFile.getRelatedType().getDescription());
        response.setRelatedId(mediaFile.getRelatedId());
        return response;
    }
}