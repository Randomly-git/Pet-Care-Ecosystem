// dto/NotificationDTO.java
package petcare.example.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知 DTO
 * 用于 API 返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;
    private String type;
    private Long userId;
    private Long actorUserId;
    private String actorUserName;
    private String actorUserAvatar;
    private Long businessId;
    private String businessType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
