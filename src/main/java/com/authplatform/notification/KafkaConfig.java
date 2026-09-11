package com.authplatform.notification;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    public static final String AUTH_EVENTS = "auth-events";

    @Bean
    public NewTopic authEventsTopic() {
        return new NewTopic(AUTH_EVENTS, 1, (short) 1);
    }
}
