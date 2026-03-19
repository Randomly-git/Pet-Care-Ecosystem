package petcare.example.community_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petcare.example.community_backend.model.Notification;
import petcare.example.community_backend.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知服务
 * 处理通知的创建、查询、标记已读等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 创建通知
     */
    @Transactional
    public Notification createNotification(Notification notification) {
        log.info("创建通知: type={}, userId={}, actorUserId={}",
                notification.getType(), notification.getUserId(), notification.getActorUserId());

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }

        return notificationRepository.save(notification);
    }

    /**
     * 批量创建通知
     */
    @Transactional
    public List<Notification> createNotifications(List<Notification> notifications) {
        log.info("批量创建通知: count={}", notifications.size());

        notifications.forEach(n -> {
            if (n.getCreatedAt() == null) {
                n.setCreatedAt(LocalDateTime.now());
            }
            if (n.getIsRead() == null) {
                n.setIsRead(false);
            }
        });

        return notificationRepository.saveAll(notifications);
    }

    /**
     * 查询用户通知列表（分页）
     */
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        log.debug("查询用户通知: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 查询用户未读通知
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        log.debug("查询用户未读通知: userId={}", userId);
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 查询用户未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 根据ID查询通知
     */
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * 标记单条通知为已读
     */
    @Transactional
    public boolean markAsRead(Long id) {
        int updated = notificationRepository.markAsReadById(id);
        if (updated > 0) {
            log.info("标记通知为已读: id={}", id);
            return true;
        }
        log.warn("标记通知为已读失败，通知不存在或已读: id={}", id);
        return false;
    }

    /**
     * 标记用户所有通知为已读
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("标记用户所有通知为已读: userId={}, updated={}", userId, updated);
        return updated;
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long id) {
        log.info("删除通知: id={}", id);
        notificationRepository.deleteById(id);
    }

    /**
     * 删除用户所有通知
     */
    @Transactional
    public void deleteAllUserNotifications(Long userId) {
        log.info("删除用户所有通知: userId={}", userId);
        notificationRepository.deleteByUserId(userId);
    }

    /**
     * 创建点赞通知
     */
    @Transactional
    public Notification createLikeNotification(Long userId, Long actorUserId,
                                                String actorUserName, String actorUserAvatar,
                                                Long momentId) {
        Notification notification = Notification.builder()
                .type("LIKE")
                .userId(userId)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .actorUserAvatar(actorUserAvatar)
                .businessId(momentId)
                .businessType("MOMENT")
                .content(actorUserName + " 赞了你的动态")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return createNotification(notification);
    }

    /**
     * 创建评论通知
     */
    @Transactional
    public Notification createCommentNotification(Long userId, Long actorUserId,
                                                   String actorUserName, String actorUserAvatar,
                                                   Long commentId, Long momentId) {
        Notification notification = Notification.builder()
                .type("COMMENT")
                .userId(userId)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .actorUserAvatar(actorUserAvatar)
                .businessId(commentId)
                .businessType("COMMENT")
                .content(actorUserName + " 评论了你的动态")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return createNotification(notification);
    }

    /**
     * 创建关注通知
     */
    @Transactional
    public Notification createFollowNotification(Long userId, Long actorUserId,
                                                  String actorUserName, String actorUserAvatar) {
        Notification notification = Notification.builder()
                .type("FOLLOW")
                .userId(userId)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .actorUserAvatar(actorUserAvatar)
                .businessId(actorUserId)
                .businessType("USER")
                .content(actorUserName + " 关注了你")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return createNotification(notification);
    }

    /**
     * 创建回复通知
     */
    @Transactional
    public Notification createReplyNotification(Long userId, Long actorUserId,
                                                 String actorUserName, String actorUserAvatar,
                                                 Long replyId, Long commentId) {
        Notification notification = Notification.builder()
                .type("REPLY")
                .userId(userId)
                .actorUserId(actorUserId)
                .actorUserName(actorUserName)
                .actorUserAvatar(actorUserAvatar)
                .businessId(replyId)
                .businessType("REPLY")
                .content(actorUserName + " 回复了你的评论")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return createNotification(notification);
    }
}
