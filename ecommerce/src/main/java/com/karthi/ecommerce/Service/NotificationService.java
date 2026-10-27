
package com.karthi.ecommerce.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.karthi.ecommerce.Model.Notification;
import com.karthi.ecommerce.Model.User;
import com.karthi.ecommerce.Repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
private final EmailService emailService;
    public NotificationService(
        NotificationRepository notificationRepository,
        UserService userService,
        EmailService emailService) {
    this.notificationRepository = notificationRepository;
    this.userService = userService;
    this.emailService = emailService;
}
    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(String email) {
        User user = userService.getUserByEmail(email);

        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userService.getUserByEmail(email);

        return notificationRepository
                .countByUser_UserIdAndReadFalse(user.getUserId());
    }

    @Transactional
    public Notification markAsRead(String email, Long notificationId) {
        User user = userService.getUserByEmail(email);

        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(
                        notificationId, user.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Notification not found"));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(String email) {
        User user = userService.getUserByEmail(email);

        List<Notification> notifications = notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());

        int updatedCount = 0;

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                updatedCount++;
            }
        }

        notificationRepository.saveAll(notifications);

        return updatedCount;
    }

    @Transactional
    
@Transactional
public Notification createNotification(
        Long userId,
        String title,
        String message,
        Notification.NotificationType type) {

    User user = userService.getUserById(userId);

    Notification notification = new Notification();
    notification.setUser(user);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setType(type);
    notification.setRead(false);

    Notification savedNotification =
            notificationRepository.save(notification);

    // Send email to the user's registered email address.
    try {
        emailService.sendEmail(
                user.getEmail(),
                title,
                message
        );
    } catch (Exception e) {
        // Keep the in-app notification even if email sending fails.
        System.err.println(
                "Email notification failed for user ID "
                        + userId + ": " + e.getMessage()
        );
    }

    return savedNotification;
}

}
