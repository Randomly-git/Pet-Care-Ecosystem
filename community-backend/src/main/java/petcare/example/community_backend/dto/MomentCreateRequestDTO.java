package petcare.example.community_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用于接收前端创建动态请求的 DTO。
 * 使用 Bean Validation 注解进行简单的输入校验。
 */
@Data
public class MomentCreateRequestDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "动态内容不能为空")
    private String content;

    // 可以在这里预留接收 media 的临时ID或标识
    // private List<String> tempMediaIds;
}