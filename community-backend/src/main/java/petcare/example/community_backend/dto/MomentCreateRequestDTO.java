package petcare.example.community_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 用于接收前端创建动态请求的 DTO。。
 */
@Data
public class MomentCreateRequestDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "动态内容不能为空")
    private String content;

    /**
     * 前端预上传媒体文件后返回的 ID 列表。
     * 可为空，Service 层在创建 Moment 后负责将这些 ID 关联到 Moment ID 上。
     */
    private List<Long> mediaIds;
}