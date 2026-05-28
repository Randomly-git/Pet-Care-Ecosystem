package petcare.example.community_backend.service;

import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.dto.CommentCreateRequestDTO;
import petcare.example.community_backend.dto.CommentResponseDTO;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.event.NotificationEvent;
import petcare.example.community_backend.event.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 引入 Slf4j 日志
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论业务服务类，负责评论的创建、查询（含层级构建和数据聚合）、删除。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PetMomentRepository petMomentRepository;
    private final LikeService likeService;
    private final UserServiceFacade userServiceFacade; // 注入用户服务门面
    private final NotificationEventPublisher notificationEventPublisher; // 注入通知发布者

    /**
     * 创建评论或回复
     * @param requestDTO 评论请求 DTO
     * @return 创建成功的评论/回复的 DTO
     */
    @Transactional
    public CommentResponseDTO createComment(CommentCreateRequestDTO requestDTO) {
        // 1. 业务校验（检查 parentId 是否存在且属于同一 moment）
        Long targetUserId = null; // 被通知的用户ID

        // 【冷迁移保护】检查目标动态是否正在迁移中
        checkMomentNotMigrating(requestDTO.getMomentId());

        if (requestDTO.getParentId() != null) {
            Comment parentComment = commentRepository.findById(requestDTO.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("回复的评论 (parentId) 不存在."));
            if (!parentComment.getMomentId().equals(requestDTO.getMomentId())) {
                throw new IllegalArgumentException("回复的评论不属于目标动态.");
            }
            // 获取被回复人的用户ID（用于发送通知）
            targetUserId = parentComment.getUserId();
        } else {
            // 顶级评论：通知动态作者
            // TODO: 需要从 MomentService 获取动态作者ID
            targetUserId = getMomentAuthorId(requestDTO.getMomentId());
        }

        // 2. DTO -> Entity 并保存
        Comment newComment = new Comment();
        newComment.setUserId(requestDTO.getUserId());
        newComment.setMomentId(requestDTO.getMomentId());
        newComment.setContent(requestDTO.getContent());
        newComment.setParentId(requestDTO.getParentId());

        Comment savedComment = commentRepository.save(newComment);

        // 3. 聚合所需的用户信息（作者 + 被回复人）
        Set<Long> userIds = new HashSet<>(Arrays.asList(savedComment.getUserId()));
        // 如果是回复，需要被回复人的 ID
        if (savedComment.getParentId() != null) {
            commentRepository.findById(savedComment.getParentId()).ifPresent(parent -> {
                userIds.add(parent.getUserId());
            });
        }
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);

        // 4. 发送评论通知（异步 MQ）
        sendCommentNotification(requestDTO, savedComment.getId(), targetUserId);

        // 5. Entity -> ResponseDTO (新建评论点赞数为 0)
        return convertToDto(savedComment, userMap, 0);
    }

    /**
     * 发送评论通知
     */
    private void sendCommentNotification(CommentCreateRequestDTO requestDTO, Long commentId, Long targetUserId) {
        if (targetUserId == null || targetUserId.equals(requestDTO.getUserId())) {
            return; // 没有目标用户或是自己
        }

        try {
            // 获取评论者信息
            Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(Set.of(requestDTO.getUserId()));
            String actorUserName = userMap.getOrDefault(requestDTO.getUserId(),
                    new UserResponseDTO(requestDTO.getUserId(), "用户" + requestDTO.getUserId(), null))
                    .getNickname();

            NotificationEvent event;
            if (requestDTO.getParentId() != null) {
                // 回复评论
                event = NotificationEvent.createReplyEvent(
                        requestDTO.getUserId(),
                        actorUserName,
                        targetUserId,
                        commentId,
                        requestDTO.getParentId()
                );
            } else {
                // 顶级评论
                event = NotificationEvent.createCommentEvent(
                        requestDTO.getUserId(),
                        actorUserName,
                        targetUserId,
                        commentId,
                        requestDTO.getMomentId()
                );
            }
            notificationEventPublisher.publishCommentNotification(event);
        } catch (Exception e) {
            log.error("发送评论通知失败: userId={}, momentId={}, error={}",
                    requestDTO.getUserId(), requestDTO.getMomentId(), e.getMessage());
        }
    }

    /**
     * 获取动态作者ID
     */
    private Long getMomentAuthorId(Long momentId) {
        return petMomentRepository.findUserIdById(momentId).orElse(null);
    }

    /**
     * 【冷迁移保护】检查目标动态是否正在迁移中
     * 如果正在迁移中，抛出异常阻止操作
     */
    private void checkMomentNotMigrating(Long momentId) {
        petMomentRepository.findById(momentId).ifPresent(moment -> {
            if ("MIGRATING".equals(moment.getMigrationStatus())) {
                log.warn("【冷迁移保护】目标动态正在迁移中，拒绝评论: momentId={}", momentId);
                throw new IllegalStateException("该动态正在迁移中，请稍后重试");
            }
        });
    }


    /**
     * GET /api/v1/comments/moment/{momentId}
     * 获取某动态下的所有评论 (实现数据聚合和层级构建)
     * @param momentId 动态ID
     * @return 评论列表 (包含嵌套回复)
     */
    public List<CommentResponseDTO> getCommentsByMomentId(Long momentId) {
        // 1. 查找所有评论，按时间升序排列
        List<Comment> allComments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);

        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所需 ID (用户 ID 和评论 ID)
        Set<Long> userIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        Map<Long, Comment> commentMap = new HashMap<>();

        // 收集所有作者ID和评论ID
        for (Comment c : allComments) {
            userIds.add(c.getUserId());
            commentIds.add(c.getId());
            commentMap.put(c.getId(), c);
        }

        // 收集所有 parentId 对应的 userId (即回复的目标用户ID)
        commentMap.values().stream()
                .filter(c -> c.getParentId() != null)
                .map(c -> commentMap.get(c.getParentId()))
                .filter(Objects::nonNull)
                .forEach(parent -> userIds.add(parent.getUserId()));

        // 3. 调用 Facade 和 Service 进行批量查询
        Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(userIds);
        Map<Long, Long> likeCounts = likeService.countLikesByTargetIds(TargetType.COMMENT, commentIds); // 假设 LikeService 已有此方法

        // 4. 构建评论层级结构并填充 DTO
        List<CommentResponseDTO> rootComments = new ArrayList<>();
        Map<Long, CommentResponseDTO> dtoMap = new HashMap<>();

        for (Comment comment : allComments) {
            // 实体转换为 DTO 并填充聚合数据
            CommentResponseDTO dto = convertToDto(
                    comment,
                    userMap,
                    likeCounts.getOrDefault(comment.getId(), 0L).intValue()
            );
            dtoMap.put(comment.getId(), dto);

            if (comment.getParentId() == null) {
                // 顶级评论
                rootComments.add(dto);
            } else {
                // 回复，将其添加到父评论的 replies 列表中
                CommentResponseDTO parentDto = dtoMap.get(comment.getParentId());
                if (parentDto != null) {
                    if (parentDto.getReplies() == null) {
                        parentDto.setReplies(new ArrayList<>());
                    }
                    parentDto.getReplies().add(dto);
                } else {
                    log.warn("发现孤立回复，ID: {}，ParentID: {}。作为顶级评论处理。", comment.getId(), comment.getParentId());
                    rootComments.add(dto); // 容错处理：作为顶级评论显示
                }
            }
        }

        return rootComments;
    }

    /**
     * 辅助方法：将 Comment 实体转换为 CommentResponseDTO，并填充聚合数据
     */
    private CommentResponseDTO convertToDto(Comment comment, Map<Long, UserResponseDTO> userMap, int likeCount) {
        CommentResponseDTO dto = new CommentResponseDTO();
        // 1. 基础数据
        dto.setId(comment.getId());
        dto.setMomentId(comment.getMomentId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setParentId(comment.getParentId());
        dto.setUserId(comment.getUserId());
        dto.setLikeCount(likeCount);

        // 2. 填充作者信息 (使用默认值进行降级处理)
        UserResponseDTO authorInfo = userMap.getOrDefault(comment.getUserId(), new UserResponseDTO(comment.getUserId(), "未知用户", null));
        dto.setAuthorName(authorInfo.getNickname());
        dto.setAuthorAvatarUrl(authorInfo.getAvatarUrl());

        // 3. 填充回复信息 (被回复人信息)
        if (comment.getParentId() != null) {
            // 再次查询父评论实体以获取其作者ID (或在调用层传入 parentCommentMap)
            commentRepository.findById(comment.getParentId()).ifPresent(parentComment -> {
                Long replyToUserId = parentComment.getUserId();
                UserResponseDTO replyToUserInfo = userMap.getOrDefault(replyToUserId, new UserResponseDTO(replyToUserId, "未知用户", null));

                dto.setReplyToUserId(replyToUserId);
                dto.setReplyToUserName(replyToUserInfo.getNickname());
            });
        }

        return dto;
    }

    /**
     * 删除某动态下的所有评论 (供 MomentService 调用) - 实现级联删除
     * @param momentId 动态ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentsByMomentId(Long momentId) {
        // 1. 查询所有待删除评论的ID
        List<Comment> commentsToDelete = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        if (commentsToDelete.isEmpty()) {
            return;
        }

        Set<Long> commentIds = commentsToDelete.stream()
                .map(Comment::getId)
                .collect(Collectors.toSet());

        // 2. 级联删除所有针对这些评论的点赞记录 (通过 LikeService 统一处理)
        likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.COMMENT, commentIds); // 假设 LikeService 已有此方法

        // 3. 删除评论主体
        commentRepository.deleteByMomentId(momentId);
    }

    /**
     * 删除单条评论 (及其所有回复和点赞)
     * @param commentId 评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        // 1. 检查评论是否存在
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在，ID: " + commentId));

        // 2. 收集需要删除的评论ID集合 (评论本身 + 所有回复它的评论)
        // 查找属于同一动态且 parentId 等于 commentId 的评论
        List<Comment> replies = commentRepository.findByMomentIdOrderByCreatedAtAsc(comment.getMomentId()).stream()
                .filter(c -> commentId.equals(c.getParentId()))
                .collect(Collectors.toList());

        Set<Long> idsToDelete = new HashSet<>();
        idsToDelete.add(commentId); // 评论本身
        replies.stream().map(Comment::getId).forEach(idsToDelete::add); // 所有回复

        // 3. 级联删除点赞记录
        likeService.deleteLikesByTargetTypeAndTargetIds(TargetType.COMMENT, idsToDelete); // 假设 LikeService 已有此方法

        // 4. 删除评论主体 (会删除 commentId 及其所有回复)
        // 使用 deleteAllById 效率更高
        commentRepository.deleteAllById(idsToDelete);
    }

    /**
     * 【新增】批量统计动态的评论数 (供 MomentService 调用)
     * @param momentIds 动态 ID 集合
     * @return Map<MomentId, Count>
     */
    public Map<Long, Long> countCommentsByMomentIds(Collection<Long> momentIds) {
        // CommentRepository 增加了批量方法：
        // List<Object[]> countByMomentIdIn(Collection<Long> momentIds);

        return commentRepository.countByMomentIdIn(momentIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // MomentId
                        arr -> (Long) arr[1]   // Count
                ));
    }
}