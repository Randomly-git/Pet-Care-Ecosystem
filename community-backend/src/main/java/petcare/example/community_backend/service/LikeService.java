package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.dto.LikeRequestDTO;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.event.NotificationEvent;
import petcare.example.community_backend.event.NotificationEventPublisher;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final LikeRepository likeRepository;
    private final PetMomentRepository petMomentRepository;
    private final CommentRepository commentRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserServiceFacade userServiceFacade;

    @Transactional
    public boolean toggleLike(LikeRequestDTO requestDTO) {
        Long userId = requestDTO.getUserId();
        TargetType targetType = requestDTO.getTargetType();
        Long targetId = requestDTO.getTargetId();

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);

        if (existingLike.isPresent()) {
            // 已点赞，执行取消点赞 (删除)
            likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
            log.debug("用户 {} 取消了 {} 类型 {} 的点赞", userId, targetType, targetId);
            return false; // 返回 false 表示已取消
        } else {
            // 未点赞，执行点赞 (创建)
            Like newLike = new Like();
            newLike.setUserId(userId);
            newLike.setTargetType(targetType);
            newLike.setTargetId(targetId);
            likeRepository.save(newLike);

            // 发送点赞通知（异步 MQ）
            sendLikeNotification(userId, targetType, targetId);

            return true; // 返回 true 表示已点赞
        }
    }

    /**
     * 发送点赞通知
     */
    private void sendLikeNotification(Long actorUserId, TargetType targetType, Long targetId) {
        try {
            // 获取触发者用户信息
            Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(Set.of(actorUserId));
            String actorUserName = userMap.getOrDefault(actorUserId,
                    new UserResponseDTO(actorUserId, "用户" + actorUserId, null))
                    .getNickname();

            Long targetUserId = null;

            // 根据目标类型获取被点赞内容的作者ID
            if (targetType == TargetType.MOMENT) {
                // TODO: 需要从 MomentService 获取动态作者ID
                // 这里暂时通过其他方式获取，后续可以优化
                targetUserId = getMomentAuthorId(targetId);
            } else if (targetType == TargetType.COMMENT) {
                // TODO: 需要从 Comment 获取评论作者ID
                targetUserId = getCommentAuthorId(targetId);
            }

            if (targetUserId != null && !targetUserId.equals(actorUserId)) {
                NotificationEvent event = NotificationEvent.createLikeEvent(
                        actorUserId,
                        actorUserName,
                        targetUserId,
                        targetId
                );
                notificationEventPublisher.publishLikeNotification(event);
            }
        } catch (Exception e) {
            log.error("发送点赞通知失败: userId={}, targetType={}, targetId={}, error={}",
                    actorUserId, targetType, targetId, e.getMessage());
        }
    }

    /**
     * 获取动态作者ID
     */
    private Long getMomentAuthorId(Long momentId) {
        return petMomentRepository.findUserIdById(momentId).orElse(null);
    }

    /**
     * 获取评论作者ID
     */
    private Long getCommentAuthorId(Long commentId) {
        return commentRepository.findById(commentId)
                .map(Comment::getUserId)
                .orElse(null);
    }

    /**
     * 批量统计指定目标类型和目标ID集合的点赞数。
     * @param targetType 目标类型 (MOMENT 或 COMMENT)
     * @param targetIds 目标ID集合
     * @return Map<TargetId, Count>
     */
    public Map<Long, Long> countLikesByTargetIds(TargetType targetType, Collection<Long> targetIds) {
        // 调用 Repository 批量查询，并将 List<Object[]> 结果转换为 Map<Long, Long>
        return likeRepository.countLikesByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // TargetId 是 Long 类型
                        arr -> (Long) arr[1]   // Count 是 Long 类型
                ));
    }

    /**
     * 批量删除指定目标类型和目标ID集合的点赞记录 (用于级联删除)
     * @param targetType 目标类型 (MOMENT 或 COMMENT)
     * @param targetIds 目标ID集合
     */
    @Transactional
    public void deleteLikesByTargetTypeAndTargetIds(TargetType targetType, Collection<Long> targetIds) {
        if (targetIds.isEmpty()) return;
        likeRepository.deleteByTargetTypeAndTargetIdIn(targetType, targetIds);
    }
}