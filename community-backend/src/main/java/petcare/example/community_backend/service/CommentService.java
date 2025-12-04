package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.dto.CommentResponseDTO;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.model.TargetType;
// 假设引入了用户服务 Facade 用于获取用户昵称和头像
// 如果您有这个类，请确保路径正确，否则需要您自行实现或 mock
// import petcare.example.community_backend.client.UserServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论业务服务类，负责评论的创建、查询（含层级构建和数据聚合）。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final LikeService likeService; // 用于点赞统计

    // **注意：UserServiceFacade 未提供，此处仅为结构示例，实际需注入并实现远程接口**
    // private final UserServiceFacade userServiceFacade;

    /**
     * 创建评论或回复
     * @param requestDTO 评论请求 DTO
     * @return 创建成功的评论/回复的 CommentResponseDTO
     */
    @Transactional
    public CommentResponseDTO createComment(CommentCreateRequestDTO requestDTO) {
        // 1. 创建 Comment 实体
        Comment comment = new Comment();
        comment.setUserId(requestDTO.getUserId());
        comment.setMomentId(requestDTO.getMomentId());
        comment.setContent(requestDTO.getContent());
        comment.setParentId(requestDTO.getParentId());

        Comment savedComment = commentRepository.save(comment);

        // 2. 转换并聚合 DTO (为简化，此处仅返回基础数据，实际项目中应补充用户/点赞信息聚合)
        // 假设聚合方法能在单次操作中处理：
        return convertToResponseDTO(savedComment, Map.of(), Map.of());
    }

    /**
     * 获取某一动态下的所有评论 (层级结构 + 聚合数据)
     * @param momentId 动态ID
     * @return 包含嵌套回复的 CommentResponseDTO 列表
     */
    public List<CommentResponseDTO> getCommentsByMomentId(Long momentId) {
        // 1. 查询所有评论和回复，按创建时间升序
        List<Comment> allComments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);

        if (allComments.isEmpty()) {
            return List.of();
        }

        // 2. 收集所有需要聚合的数据 (用户ID、评论ID)
        Set<Long> commentIds = allComments.stream().map(Comment::getId).collect(Collectors.toSet());
        Set<Long> userIds = allComments.stream().map(Comment::getUserId).collect(Collectors.toSet());

        // 获取所有父评论 (用于确定回复对象的用户ID)
        Map<Long, Comment> parentCommentMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .map(Comment::getParentId)
                .distinct()
                .flatMap(parentId -> commentRepository.findById(parentId).stream())
                .collect(Collectors.toMap(Comment::getId, Function.identity()));

        // 收集被回复人的用户ID
        parentCommentMap.values().stream()
                .map(Comment::getUserId)
                .forEach(userIds::add);

        // 3. 聚合远程数据 (此处为示例，实际应调用 LikeService/UserServiceFacade)

        // **点赞数聚合 (假设 LikeService 提供了批量查询方法)**
        // Map<Long, Integer> likeCounts = likeService.getLikeCounts(TargetType.COMMENT, commentIds);
        Map<Long, Integer> likeCounts = commentIds.stream()
                .collect(Collectors.toMap(id -> id, id -> (int) (id % 5 + 1))); // 模拟点赞数

        // **用户信息聚合 (假设 UserServiceFacade 提供了批量查询方法)**
        // Map<Long, UserInfoDTO> userInfos = userServiceFacade.getUserInfo(userIds);
        Map<Long, Map<String, String>> userInfos = userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> Map.of(
                                "name", "用户" + id,
                                "avatarUrl", "/avatar/" + id + ".jpg"
                        )
                )); // 模拟用户信息 Map<UserId, Map<Key, Value>>


        // 4. 转换为 DTO 并构建层级结构
        Map<Long, CommentResponseDTO> dtoMap = allComments.stream()
                .map(comment -> convertToResponseDTO(comment, userInfos, parentCommentMap))
                .peek(dto -> dto.setLikeCount(likeCounts.getOrDefault(dto.getId(), 0))) // 填充点赞数
                .collect(Collectors.toMap(CommentResponseDTO::getId, Function.identity()));

        // 5. 嵌套回复
        List<CommentResponseDTO> topLevelComments = dtoMap.values().stream()
                .filter(dto -> dto.getParentId() == null)
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // 最新的一级评论在最上面
                .collect(Collectors.toList());

        // 将回复（ParentId 非空）添加到对应的一级评论下
        dtoMap.values().stream()
                .filter(dto -> dto.getParentId() != null)
                .forEach(reply -> {
                    // 假设 ParentId 总是指向顶级评论的 ID
                    Long topLevelId = reply.getParentId();

                    if (dtoMap.containsKey(topLevelId)) {
                        CommentResponseDTO topComment = dtoMap.get(topLevelId);
                        if (topComment.getReplies() == null) {
                            topComment.setReplies(new java.util.ArrayList<>());
                        }
                        topComment.getReplies().add(reply);
                    }
                });

        // 6. 调整回复的排序：按创建时间升序 (旧回复在前面)
        topLevelComments.forEach(topComment -> {
            if (topComment.getReplies() != null) {
                topComment.getReplies().sort((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()));
            }
        });

        return topLevelComments;
    }

    // 简化的私有方法：将 Comment 实体转换为 CommentResponseDTO 并填充用户/回复信息
    private CommentResponseDTO convertToResponseDTO(Comment comment,
                                                    Map<Long, Map<String, String>> userInfos,
                                                    Map<Long, Comment> parentCommentMap) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setMomentId(comment.getMomentId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setParentId(comment.getParentId());
        dto.setUserId(comment.getUserId());

        // 填充作者信息
        Map<String, String> authorInfo = userInfos.getOrDefault(comment.getUserId(), Map.of());
        dto.setAuthorName(authorInfo.getOrDefault("name", "未知用户"));
        dto.setAuthorAvatarUrl(authorInfo.getOrDefault("avatarUrl", ""));

        // 填充回复信息 (如果存在 parentId)
        if (comment.getParentId() != null) {
            Comment parentComment = parentCommentMap.get(comment.getParentId());
            if (parentComment != null) {
                Long replyToUserId = parentComment.getUserId();
                Map<String, String> replyToUserInfo = userInfos.getOrDefault(replyToUserId, Map.of());

                dto.setReplyToUserId(replyToUserId);
                dto.setReplyToUserName(replyToUserInfo.getOrDefault("name", "未知用户"));
            }
        }

        return dto;
    }

    /**
     * 删除某动态下的所有评论 (供 MomentService 调用)
     * @param momentId 动态ID
     */
    @Transactional
    public void deleteCommentsByMomentId(Long momentId) {
        // TODO: 实际应用中，还需要在删除评论的同时，删除所有针对这些评论的点赞记录。
        // likeService.deleteLikesByTargetIdAndType(TargetType.COMMENT, commentIds);

        commentRepository.deleteByMomentId(momentId);
    }

    /**
     * 删除单条评论 (及其所有回复)
     * @param commentId 评论ID
     */
    @Transactional
    public void deleteComment(Long commentId) {
        // TODO: 实现删除逻辑
        // 1. 查找所有以该 commentId 为 parentId 的回复（如果有）
        // 2. 删除所有回复
        // 3. 删除原始评论
        // 4. 删除所有相关点赞

        // 简化处理：仅删除原始评论
        commentRepository.deleteById(commentId);
    }
}