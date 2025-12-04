package petcare.example.community_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论的响应 DTO，用于向前端提供数据，支持一级嵌套（顶级评论包含回复列表）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    // --- 评论/回复 主体数据 ---
    private Long id;
    private Long momentId;
    private String content;
    private LocalDateTime createdAt;

    // parentId 用于区分是顶级评论(null)还是回复(非null)，也是关联回复目标。
    private Long parentId;

    // --- 评论作者信息 (聚合自 User 微服务) ---
    private Long userId;
    private String authorName;      // 作者昵称
    private String authorAvatarUrl; // 作者头像 URL

    // --- 针对回复：被回复人信息 (聚合自 User 微服务) ---
    // 只有当 parentId 非空时，此字段才有意义。它指向 parentId 评论的作者。
    private Long replyToUserId;
    private String replyToUserName; // 被回复人的昵称 (即 parentId 对应的评论的作者)

    // --- 互动计数 (聚合自 Like/Interaction 微服务) ---
    private int likeCount = 0; // 该评论/回复收到的点赞数

    // --- 嵌套回复列表 (仅用于顶级评论，包含第一级回复) ---
    // 由于只支持一层嵌套，此列表中的 CommentResponseDTO.replies 在 Service 中将始终为空。
    private List<CommentResponseDTO> replies;
}