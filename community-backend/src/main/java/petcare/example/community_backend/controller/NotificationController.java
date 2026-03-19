// controller/NotificationController.java
package petcare.example.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petcare.example.community_backend.dto.NotificationDTO;
import petcare.example.community_backend.model.Notification;
import petcare.example.community_backend.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知控制器
 * 提供通知查询和管理的 API
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "用户通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取用户通知列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表（分页）")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        log.info("获取用户通知列表: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationService.getUserNotifications(userId, pageable);

        // 转换为 DTO
        List<NotificationDTO> notifications = notificationPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        // 获取未读数量
        long unreadCount = notificationService.getUnreadCount(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("notifications", notifications);
        result.put("totalElements", notificationPage.getTotalElements());
        result.put("totalPages", notificationPage.getTotalPages());
        result.put("unreadCount", unreadCount);
        result.put("currentPage", page);
        result.put("pageSize", size);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户未读通知数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量", description = "获取当前用户未读通知的数量")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        long count = notificationService.getUnreadCount(userId);
        log.info("用户未读通知数量: userId={}, count={}", userId, count);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("unreadCount", count);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户未读通知列表
     */
    @GetMapping("/unread")
    @Operation(summary = "获取未读通知列表", description = "获取当前用户未读通知列表")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        log.info("获取用户未读通知: userId={}", userId);

        List<Notification> notifications = notificationService.getUnreadNotifications(userId);

        List<NotificationDTO> dtos = notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long id) {

        log.info("标记通知为已读: id={}", id);

        boolean success = notificationService.markAsRead(id);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("success", success);
        result.put("message", success ? "标记成功" : "通知不存在或已读");

        return ResponseEntity.ok(result);
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读", description = "将用户所有未读通知标记为已读")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @Parameter(description = "用户ID") @RequestParam Long userId) {

        log.info("标记所有通知为已读: userId={}", userId);

        int updated = notificationService.markAllAsRead(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("updatedCount", updated);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * 删除指定通知
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable Long id) {

        log.info("删除通知: id={}", id);

        notificationService.deleteNotification(id);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * 删除用户所有通知
     */
    @DeleteMapping("/user/{userId}")
    @Operation(summary = "删除用户所有通知", description = "删除用户的所有通知")
    public ResponseEntity<Map<String, Object>> deleteAllUserNotifications(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.info("删除用户所有通知: userId={}", userId);

        notificationService.deleteAllUserNotifications(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * 转换为 DTO
     */
    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .userId(notification.getUserId())
                .actorUserId(notification.getActorUserId())
                .actorUserName(notification.getActorUserName())
                .actorUserAvatar(notification.getActorUserAvatar())
                .businessId(notification.getBusinessId())
                .businessType(notification.getBusinessType())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
