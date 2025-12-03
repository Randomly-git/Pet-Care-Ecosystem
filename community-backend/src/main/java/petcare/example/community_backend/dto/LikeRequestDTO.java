package petcare.example.community_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import petcare.example.community_backend.model.TargetType;

@Data
public class LikeRequestDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "目标类型不能为空")
    private TargetType targetType; // MOMENT 或 COMMENT
    @NotNull(message = "目标ID不能为空")
    private Long targetId; // 动态ID 或 评论ID
}