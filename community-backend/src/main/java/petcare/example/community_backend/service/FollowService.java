package petcare.example.community_backend.service;

import petcare.example.community_backend.model.Follow;
import petcare.example.community_backend.dto.FollowRequestDTO;
import petcare.example.community_backend.repository.FollowRepository;
import petcare.example.community_backend.event.NotificationEvent;
import petcare.example.community_backend.event.NotificationEventPublisher;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserServiceFacade userServiceFacade;

    @Transactional
    public boolean toggleFollow(FollowRequestDTO requestDTO) {
        Long followerId = requestDTO.getFollowerId();
        Long followedId = requestDTO.getFollowedId();

        // 校验：用户不能关注自己
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("用户不能关注自己");
        }

        Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowedId(followerId, followedId);

        if (existingFollow.isPresent()) {
            // 已关注，执行取消关注 (删除)
            followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
            log.debug("用户 {} 取消了关注 {}", followerId, followedId);
            return false; // 已取消关注
        } else {
            // 未关注，执行关注 (创建)
            Follow newFollow = new Follow();
            newFollow.setFollowerId(followerId);
            newFollow.setFollowedId(followedId);
            followRepository.save(newFollow);

            // 发送关注通知（异步 MQ）
            sendFollowNotification(followerId, followedId);

            return true; // 已关注
        }
    }

    /**
     * 发送关注通知
     */
    private void sendFollowNotification(Long actorUserId, Long targetUserId) {
        try {
            // 获取触发者用户信息
            Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(Set.of(actorUserId));
            String actorUserName = userMap.getOrDefault(actorUserId,
                    new UserResponseDTO(actorUserId, "用户" + actorUserId, null))
                    .getNickname();

            NotificationEvent event = NotificationEvent.createFollowEvent(
                    actorUserId,
                    actorUserName,
                    targetUserId
            );
            notificationEventPublisher.publishFollowNotification(event);
        } catch (Exception e) {
            log.error("发送关注通知失败: followerId={}, followedId={}, error={}",
                    actorUserId, targetUserId, e.getMessage());
        }
    }

    public long getFollowerCount(Long userId) {
        // 统计粉丝数
        return followRepository.countByFollowedId(userId);
    }

    public long getFollowingCount(Long userId) {
        // 统计关注数
        return followRepository.countByFollowerId(userId);
    }
}