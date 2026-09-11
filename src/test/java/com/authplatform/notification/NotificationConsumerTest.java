package com.authplatform.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock NotificationRepository notificationRepository;
    @Mock MailService mailService;

    @InjectMocks NotificationConsumer consumer;

    @Test
    void consumer_persistsInboxAndSendsMail() {
        AuthEvent e = new AuthEvent("user-registered", "n@x.com", "New User",
                LocalDateTime.now(), UUID.randomUUID().toString());

        consumer.onAuthEvent(e);

        verify(notificationRepository).save(any(Notification.class));
        verify(mailService).send(any(), any(), any());
    }
}
