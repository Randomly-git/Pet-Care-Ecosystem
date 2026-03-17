// dto/MediaBindEvent.java
package petcare.example.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 媒体关联事件消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaBindEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 事件类型: ACTIVITY_RECORD, STATUS_RECORD, COMMUNITY_MOMENT
     */
    private String eventType;

    /**
     * 关联的业务ID
     */
    private Long businessId;

    /**
     * 媒体文件ID列表
     */
    private List<Long> mediaIds;

    /**
     * 关联类型
     */
    private String relatedType;

    /**
     * 发起请求的用户ID
     */
    private Long userId;

    /**
     * 发起时间
     */
    private LocalDateTime timestamp;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;
}
