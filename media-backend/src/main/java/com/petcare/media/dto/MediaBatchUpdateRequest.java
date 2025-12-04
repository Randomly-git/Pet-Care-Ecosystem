// media-backend/src/main/java/com/petcare/media/dto/MediaBatchUpdateRequest.java
package com.petcare.media.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 媒体文件批量更新关联ID的请求 DTO
 */
@Data
public class MediaBatchUpdateRequest {

    @NotEmpty(message = "媒体ID列表不能为空")
    private List<Long> mediaIds;

    @NotBlank(message = "关联类型不能为空") // 例如：MOMENT, USER_AVATAR
    private String relatedType;

    @NotNull(message = "新的关联ID不能为空")
    private Long newRelatedId;
}