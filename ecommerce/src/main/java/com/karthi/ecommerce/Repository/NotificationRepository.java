
package com.karthi.ecommerce.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.karthi.ecommerce.Model.Notification;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(
            Long userId);

    long countByUser_UserIdAndReadFalse(Long userId);

    Optional<Notification> findByNotificationIdAndUser_UserId(
            Long notificationId, Long userId);
}
