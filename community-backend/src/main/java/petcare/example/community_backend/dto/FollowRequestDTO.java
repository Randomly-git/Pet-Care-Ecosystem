package petcare.example.community_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class FollowRequestDTO {
    @NotNull(message = "关注者ID不能为空")
    private Long followerId;

    @NotNull(message = "被关注者ID不能为空")
    private Long followedId;
}