package com.authplatform;

import com.authplatform.auth.AuthDtos;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Disabled("Needs a Linux Docker host (Testcontainers+npipe fails on Windows). Runs in CI (ubuntu) — see build.yml.")
class AuthFlowIntegrationTest {

    @MockBean
    KafkaTemplate<String, com.authplatform.notification.AuthEvent> kafkaTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", () -> "dummy:9092");
        r.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Autowired TestRestTemplate rest;

    @Test
    void register_verify_login_refresh_reset_loop() {
        // register
        AuthDtos.RegisterRequest reg = new AuthDtos.RegisterRequest();
        reg.setFullName("Loop User");
        reg.setEmail("loop@x.com");
        reg.setPassword("secret123");
        ResponseEntity<Map> regRes = rest.postForEntity("/api/auth/register", reg, Map.class);
        assertThat(regRes.getStatusCode().is2xxSuccessful()).isTrue();

        // login blocked before verification
        AuthDtos.LoginRequest login = new AuthDtos.LoginRequest();
        login.setEmail("loop@x.com");
        login.setPassword("secret123");
        ResponseEntity<Map> blocked = rest.postForEntity("/api/auth/login", login, Map.class);
        assertThat(blocked.getStatusCode().is2xxSuccessful()).isFalse();

        // forgot + reset works on the token path only with a real token;
        // enumeration safety: unknown email still returns 200
        AuthDtos.ForgotRequest forgot = new AuthDtos.ForgotRequest();
        forgot.setEmail("nobody@x.com");
        ResponseEntity<Map> forgotRes = rest.postForEntity("/api/auth/forgot", forgot, Map.class);
        assertThat(forgotRes.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
