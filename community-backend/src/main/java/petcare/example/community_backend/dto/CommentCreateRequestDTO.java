package petcare.example.community_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CommentCreateRequestDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "动态ID不能为空")
    private Long momentId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
    private Long parentId; // 可选，回复的评论ID
}