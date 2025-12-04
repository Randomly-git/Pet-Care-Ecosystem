package petcare.example.community_backend.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// 镜像媒体微服务的 ApiResponse 结构
@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}