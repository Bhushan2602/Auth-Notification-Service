package com.authplatform.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    @KafkaListener(topics = KafkaConfig.AUTH_EVENTS, groupId = "auth-notifications")
    public void onAuthEvent(AuthEvent event) {
        String message = switch (event.getType()) {
            case "user-registered" -> "Welcome " + event.getFullName() + "! Verify your email to activate your account.";
            case "password-reset" -> "A password reset was requested. Use the link sent to your email (expires in 1 hour).";
            case "password-changed" -> "Your password was just changed. Contact support if this wasn't you.";
            case "email-verified" -> "Your email is verified. Welcome aboard!";
            default -> "Account update: " + event.getType();
        };

        Notification n = new Notification();
        n.setEmail(event.getEmail());
        n.setType(event.getType());
        n.setMessage(message);
        notificationRepository.save(n);

        mailService.send(event.getEmail(), "[AuthPlatform] " + event.getType(), message);
    }
}
