// dto/MediaResponse.java
package com.petcare.backend.dto.response;

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
    private Long userId;
    private String relatedType;
    private String relatedTypeDesc;
    private Long relatedId;
}