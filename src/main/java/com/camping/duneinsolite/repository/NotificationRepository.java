package com.camping.duneinsolite.repository;

import com.camping.duneinsolite.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserUserId(UUID userId);
    List<Notification> findByUserUserIdAndIsReadFalse(UUID userId);
}