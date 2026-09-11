package com.authplatform.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {
    private String type;
    private String email;
    private String fullName;
    private LocalDateTime occurredAt;
    private String eventId;
}
