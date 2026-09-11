package com.authplatform.auth;

import com.authplatform.security.JwtUtil;
import com.authplatform.security.RateLimitService;
import com.authplatform.session.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthTokenRepository tokenRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock SessionService sessionService;
    @Mock RateLimitService rateLimitService;
    @Mock KafkaTemplate<String, com.authplatform.notification.AuthEvent> kafkaTemplate;

    @InjectMocks AuthService authService;

    private AuthDtos.RegisterRequest reg(String email) {
        AuthDtos.RegisterRequest r = new AuthDtos.RegisterRequest();
        r.setFullName("Test User");
        r.setEmail(email);
        r.setPassword("secret123");
        return r;
    }

    @Test
    void register_succeeds_forNewEmail() {
        when(userRepository.existsByEmail("n@x.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        authService.register(reg("n@x.com"));

        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("d@x.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(reg("d@x.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_rejectsUnverifiedEmail() {
        User u = new User();
        u.setEmail("u@x.com");
        u.setPassword("hashed");
        u.setEmailVerified(false);
        when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("u@x.com");
        req.setPassword("secret123");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void login_failurePath_isIdenticalForUnknownUser() {
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("ghost@x.com");
        req.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .hasMessageContaining("Invalid email or password");
        verify(rateLimitService).recordFailure(eq("ghost@x.com"), any(), eq("127.0.0.1"));
    }

    @Test
    void setRole_rejectsNonAdminRoles() {
        assertThatThrownBy(() -> authService.setRole("a@x.com", "ROLE_SUPERUSER"))
                .hasMessageContaining("ROLE_USER or ROLE_ADMIN");
    }

    @Test
    void changePassword_rejectsWrongCurrent() {
        User u = new User();
        u.setEmail("u@x.com");
        u.setPassword("hashed");
        when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(eq("wrong"), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("u@x.com", "wrong", "newpass123"))
                .hasMessageContaining("incorrect");
    }
}
