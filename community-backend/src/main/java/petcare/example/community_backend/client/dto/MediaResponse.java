package petcare.example.community_backend.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// 镜像媒体微服务的 MediaResponse 结构
@Data
@NoArgsConstructor
public class MediaResponse {
    private Long mediaId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long relatedId;
    // ...
}