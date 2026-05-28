package petcare.example.community_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import petcare.example.community_backend.dto.NotificationDTO;
import petcare.example.community_backend.model.Notification;
import petcare.example.community_backend.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Notification 控制器测试
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    private final String BASE_URL = "/notifications";

    // ==================== 获取通知列表测试 ====================

    @Test
    void getNotifications_ValidRequest_ReturnsNotificationList() throws Exception {
        Long userId = 1L;
        Notification notification = createTestNotification(1L, userId, "LIKE");
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);

        when(notificationService.getUserNotifications(eq(userId), any())).thenReturn(page);
        when(notificationService.getUnreadCount(userId)).thenReturn(1L);

        mockMvc.perform(get(BASE_URL)
                        .param("userId", String.valueOf(userId))
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.unreadCount").value(1));

        verify(notificationService, times(1)).getUserNotifications(eq(userId), any());
        verify(notificationService, times(1)).getUnreadCount(userId);
    }

    @Test
    void getNotifications_DefaultPagination_UsesPageSize20() throws Exception {
        Long userId = 1L;
        Page<Notification> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(notificationService.getUserNotifications(eq(userId), any())).thenReturn(page);
        when(notificationService.getUnreadCount(userId)).thenReturn(0L);

        mockMvc.perform(get(BASE_URL)
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(20));

        verify(notificationService, times(1)).getUserNotifications(eq(userId), any());
    }

    // ==================== 获取未读数量测试 ====================

    @Test
    void getUnreadCount_ValidRequest_ReturnsCount() throws Exception {
        Long userId = 1L;
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        mockMvc.perform(get(BASE_URL + "/unread-count")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.unreadCount").value(5));

        verify(notificationService, times(1)).getUnreadCount(userId);
    }

    @Test
    void getUnreadCount_NoUnread_ReturnsZero() throws Exception {
        Long userId = 1L;
        when(notificationService.getUnreadCount(userId)).thenReturn(0L);

        mockMvc.perform(get(BASE_URL + "/unread-count")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    // ==================== 获取未读列表测试 ====================

    @Test
    void getUnreadNotifications_ValidRequest_ReturnsUnreadList() throws Exception {
        Long userId = 1L;
        List<Notification> notifications = List.of(
                createTestNotification(1L, userId, "LIKE"),
                createTestNotification(2L, userId, "COMMENT")
        );

        when(notificationService.getUnreadNotifications(userId)).thenReturn(notifications);

        mockMvc.perform(get(BASE_URL + "/unread")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("LIKE"))
                .andExpect(jsonPath("$[1].type").value("COMMENT"));

        verify(notificationService, times(1)).getUnreadNotifications(userId);
    }

    @Test
    void getUnreadNotifications_EmptyList_ReturnsEmptyArray() throws Exception {
        Long userId = 1L;
        when(notificationService.getUnreadNotifications(userId)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/unread")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== 标记已读测试 ====================

    @Test
    void markAsRead_ExistingNotification_ReturnsSuccess() throws Exception {
        Long notificationId = 1L;
        when(notificationService.markAsRead(notificationId)).thenReturn(true);

        mockMvc.perform(put(BASE_URL + "/{id}/read", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("标记成功"));

        verify(notificationService, times(1)).markAsRead(notificationId);
    }

    @Test
    void markAsRead_NonExistingNotification_ReturnsFailure() throws Exception {
        Long notificationId = 999L;
        when(notificationService.markAsRead(notificationId)).thenReturn(false);

        mockMvc.perform(put(BASE_URL + "/{id}/read", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("通知不存在或已读"));
    }

    // ==================== 标记全部已读测试 ====================

    @Test
    void markAllAsRead_ValidRequest_ReturnsUpdatedCount() throws Exception {
        Long userId = 1L;
        when(notificationService.markAllAsRead(userId)).thenReturn(10);

        mockMvc.perform(put(BASE_URL + "/read-all")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.updatedCount").value(10))
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).markAllAsRead(userId);
    }

    @Test
    void markAllAsRead_NoUnreadNotifications_ReturnsZero() throws Exception {
        Long userId = 1L;
        when(notificationService.markAllAsRead(userId)).thenReturn(0);

        mockMvc.perform(put(BASE_URL + "/read-all")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(0));
    }

    // ==================== 删除通知测试 ====================

    @Test
    void deleteNotification_ExistingId_ReturnsSuccess() throws Exception {
        Long notificationId = 1L;
        doNothing().when(notificationService).deleteNotification(notificationId);

        mockMvc.perform(delete(BASE_URL + "/{id}", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).deleteNotification(notificationId);
    }

    // ==================== 删除用户所有通知测试 ====================

    @Test
    void deleteAllUserNotifications_ValidRequest_ReturnsSuccess() throws Exception {
        Long userId = 1L;
        doNothing().when(notificationService).deleteAllUserNotifications(userId);

        mockMvc.perform(delete(BASE_URL + "/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).deleteAllUserNotifications(userId);
    }

    // ==================== 辅助方法 ====================

    private Notification createTestNotification(Long id, Long userId, String type) {
        return Notification.builder()
                .id(id)
                .type(type)
                .userId(userId)
                .actorUserId(2L)
                .actorUserName("测试用户")
                .actorUserAvatar("http://example.com/avatar.jpg")
                .businessId(100L)
                .businessType("MOMENT")
                .content("测试通知内容")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
