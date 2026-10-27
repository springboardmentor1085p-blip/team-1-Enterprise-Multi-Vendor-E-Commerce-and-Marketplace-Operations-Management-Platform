
package com.karthi.ecommerce.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.karthi.ecommerce.Model.Notification;
import com.karthi.ecommerce.Service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyNotifications(
            Authentication authentication) {

        List<Map<String, Object>> response = notificationService
                .getMyNotifications(authentication.getName())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            Authentication authentication) {

        long count = notificationService
                .getUnreadCount(authentication.getName());

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        Notification notification = notificationService.markAsRead(
                authentication.getName(), notificationId);

        return ResponseEntity.ok(toResponse(notification));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            Authentication authentication) {

        int updatedCount = notificationService
                .markAllAsRead(authentication.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Notifications marked as read",
                "updatedCount", updatedCount));
    }

    private Map<String, Object> toResponse(Notification notification) {
        return Map.of(
                "notificationId", notification.getNotificationId(),
                "title", notification.getTitle(),
                "message", notification.getMessage(),
                "type", notification.getType().name(),
                "read", notification.isRead(),
                "createdAt", notification.getCreatedAt().toString());
    }
}
