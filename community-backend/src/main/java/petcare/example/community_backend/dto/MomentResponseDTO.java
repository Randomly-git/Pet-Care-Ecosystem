package petcare.example.community_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态的响应 DTO，用于向前端提供数据。
 * 包含前端需要的附加字段，如媒体URL和计数。
 */
@Data
public class MomentResponseDTO {
    private Long id;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    // 从媒体微服务获取 URL 列表
    private List<String> mediaUrls;
    private int commentCount = 0;
    private int likeCount = 0;
}