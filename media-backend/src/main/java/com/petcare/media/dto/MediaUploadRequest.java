// dto/MediaUploadRequest.java
package com.petcare.media.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MediaUploadRequest {
    private MultipartFile file;
    private Long petId;
    private String relatedType;  // 接收字符串，如 "ACTIVITY"
    private Long relatedId;
}