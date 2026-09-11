package com.authplatform.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEmailOrderByCreatedAtDesc(String email);
    long countByEmailAndReadFalse(String email);
}
