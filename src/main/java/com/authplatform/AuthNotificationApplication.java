package com.authplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@EnableKafka
@EnableScheduling
public class AuthNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthNotificationApplication.class, args);
    }
}
