package petcare.example.community_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import petcare.example.community_backend.model.Notification;
import petcare.example.community_backend.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .type("LIKE")
                .userId(100L)
                .actorUserId(200L)
                .actorUserName("测试用户")
                .actorUserAvatar("http://example.com/avatar.jpg")
                .businessId(1000L)
                .businessType("MOMENT")
                .content("测试用户 赞了你的动态")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== 创建通知测试 ====================

    @Test
    void createNotification_ValidNotification_ReturnsSavedNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createNotification(testNotification);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("LIKE");
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_NullCreatedAt_SetsCurrentTime() {
        testNotification.setCreatedAt(null);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            assertThat(saved.getCreatedAt()).isNotNull();
            return saved;
        });

        notificationService.createNotification(testNotification);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotification_NullIsRead_SetsFalse() {
        testNotification.setIsRead(null);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            assertThat(saved.getIsRead()).isFalse();
            return saved;
        });

        notificationService.createNotification(testNotification);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotifications_MultipleNotifications_ReturnsSavedList() {
        List<Notification> notifications = List.of(testNotification,
                Notification.builder().type("COMMENT").userId(100L).actorUserId(300L).build());

        when(notificationRepository.saveAll(anyList())).thenReturn(notifications);

        List<Notification> results = notificationService.createNotifications(notifications);

        assertThat(results).hasSize(2);
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    // ==================== 查询通知测试 ====================

    @Test
    void getUserNotifications_ValidUserId_ReturnsPagedNotifications() {
        Long userId = 100L;
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(testNotification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<Notification> result = notificationService.getUserNotifications(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Test
    void getUnreadNotifications_ValidUserId_ReturnsUnreadList() {
        Long userId = 100L;
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(testNotification));

        List<Notification> results = notificationService.getUnreadNotifications(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsRead()).isFalse();
        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUnreadCount_ValidUserId_ReturnsCount() {
        Long userId = 100L;
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository, times(1)).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void getNotificationById_ExistingId_ReturnsNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        Optional<Notification> result = notificationService.getNotificationById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(notificationRepository, times(1)).findById(1L);
    }

    @Test
    void getNotificationById_NonExistingId_ReturnsEmpty() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Notification> result = notificationService.getNotificationById(999L);

        assertThat(result).isEmpty();
        verify(notificationRepository, times(1)).findById(999L);
    }

    // ==================== 标记已读测试 ====================

    @Test
    void markAsRead_ExistingNotification_ReturnsTrue() {
        when(notificationRepository.markAsReadById(1L)).thenReturn(1);

        boolean result = notificationService.markAsRead(1L);

        assertThat(result).isTrue();
        verify(notificationRepository, times(1)).markAsReadById(1L);
    }

    @Test
    void markAsRead_NonExistingNotification_ReturnsFalse() {
        when(notificationRepository.markAsReadById(999L)).thenReturn(0);

        boolean result = notificationService.markAsRead(999L);

        assertThat(result).isFalse();
        verify(notificationRepository, times(1)).markAsReadById(999L);
    }

    @Test
    void markAllAsRead_ValidUserId_ReturnsUpdatedCount() {
        Long userId = 100L;
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(10);

        int result = notificationService.markAllAsRead(userId);

        assertThat(result).isEqualTo(10);
        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }

    @Test
    void markAllAsRead_NoUnread_ReturnsZero() {
        Long userId = 100L;
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(0);

        int result = notificationService.markAllAsRead(userId);

        assertThat(result).isEqualTo(0);
    }

    // ==================== 删除通知测试 ====================

    @Test
    void deleteNotification_ValidId_CallsRepository() {
        doNothing().when(notificationRepository).deleteById(1L);

        notificationService.deleteNotification(1L);

        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteAllUserNotifications_ValidUserId_CallsRepository() {
        Long userId = 100L;
        doNothing().when(notificationRepository).deleteByUserId(userId);

        notificationService.deleteAllUserNotifications(userId);

        verify(notificationRepository, times(1)).deleteByUserId(userId);
    }

    // ==================== 快捷方法测试 ====================

    @Test
    void createLikeNotification_ReturnsCorrectNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Notification result = notificationService.createLikeNotification(
                100L, 200L, "测试用户", "http://avatar.jpg", 1000L);

        assertThat(result.getType()).isEqualTo("LIKE");
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getActorUserId()).isEqualTo(200L);
        assertThat(result.getActorUserName()).isEqualTo("测试用户");
        assertThat(result.getBusinessId()).isEqualTo(1000L);
        assertThat(result.getBusinessType()).isEqualTo("MOMENT");
        assertThat(result.getIsRead()).isFalse();
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createCommentNotification_ReturnsCorrectNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Notification result = notificationService.createCommentNotification(
                100L, 200L, "测试用户", "http://avatar.jpg", 500L, 1000L);

        assertThat(result.getType()).isEqualTo("COMMENT");
        assertThat(result.getBusinessType()).isEqualTo("COMMENT");
        assertThat(result.getBusinessId()).isEqualTo(500L);
    }

    @Test
    void createFollowNotification_ReturnsCorrectNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Notification result = notificationService.createFollowNotification(
                100L, 200L, "测试用户", "http://avatar.jpg");

        assertThat(result.getType()).isEqualTo("FOLLOW");
        assertThat(result.getBusinessType()).isEqualTo("USER");
        assertThat(result.getBusinessId()).isEqualTo(200L);
        assertThat(result.getContent()).contains("关注了你");
    }

    @Test
    void createReplyNotification_ReturnsCorrectNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Notification result = notificationService.createReplyNotification(
                100L, 200L, "测试用户", "http://avatar.jpg", 600L, 500L);

        assertThat(result.getType()).isEqualTo("REPLY");
        assertThat(result.getBusinessType()).isEqualTo("REPLY");
        assertThat(result.getContent()).contains("回复了你的评论");
    }
}
