// dto/MediaBatchUpdateRequest.java
package com.petcare.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class MediaBatchUpdateRequest {
    @NotEmpty(message = "媒体ID列表不能为空")
    private List<Long> mediaIds;

    @NotBlank(message = "关联类型不能为空")
    private String relatedType;

    @NotNull(message = "新的关联ID不能为空")
    private Long newRelatedId;
}