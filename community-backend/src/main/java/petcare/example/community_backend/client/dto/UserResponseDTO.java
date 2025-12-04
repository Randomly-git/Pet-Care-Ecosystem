package petcare.example.community_backend.client.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 镜像用户微服务的用户响应 DTO，用于在 Community Service 中进行数据聚合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    // ... 其他需要的用户字段
}