package com.authplatform.auth;

import com.authplatform.notification.AuthEvent;
import com.authplatform.notification.KafkaConfig;
import com.authplatform.security.JwtUtil;
import com.authplatform.security.RateLimitService;
import com.authplatform.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;

    public void register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setProvider("LOCAL");
        user.setRole("ROLE_USER");
        userRepository.save(user);

        issueToken(req.getEmail(), AuthToken.Type.EMAIL_VERIFY, 24);
        publish("user-registered", req.getEmail(), req.getFullName());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req, String ip) {
        rateLimitService.checkAllowed(req.getEmail());
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // Identical failure path blocks email enumeration
            rateLimitService.recordFailure(req.getEmail(), "bad credentials", ip);
            throw new RuntimeException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            rateLimitService.recordFailure(req.getEmail(), "unverified email", ip);
            throw new RuntimeException("Email not verified. Check your inbox for the verification link.");
        }
        rateLimitService.recordSuccess(req.getEmail(), ip);
        String access = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refresh = sessionService.issue(user.getEmail(), req.getDeviceLabel());
        return new AuthDtos.AuthResponse(access, refresh, user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthDtos.AuthResponse refresh(String rawRefreshToken) {
        String rotated = sessionService.rotate(rawRefreshToken);
        String email = sessionService.ownerOf(rotated);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String access = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        return new AuthDtos.AuthResponse(access, rotated, user.getEmail(), user.getFullName(), user.getRole());
    }

    public void verifyEmail(String rawToken) {
        AuthToken t = tokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now()) || t.getType() != AuthToken.Type.EMAIL_VERIFY) {
            throw new RuntimeException("Invalid or expired token");
        }
        User user = userRepository.findByEmail(t.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        t.setUsed(true);
        tokenRepository.save(t);
        publish("email-verified", user.getEmail(), user.getFullName());
    }

    public void forgotPassword(String email) {
        // Same response whether or not the account exists (anti-enumeration)
        userRepository.findByEmail(email).ifPresent(user -> {
            issueToken(email, AuthToken.Type.PASSWORD_RESET, 1);
            publish("password-reset", email, user.getFullName());
        });
    }

    public void resetPassword(String rawToken, String newPassword) {
        AuthToken t = tokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
        if (t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now()) || t.getType() != AuthToken.Type.PASSWORD_RESET) {
            throw new RuntimeException("Invalid or expired token");
        }
        User user = userRepository.findByEmail(t.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        t.setUsed(true);
        tokenRepository.save(t);
        sessionService.revokeAll(user.getEmail());
        publish("password-changed", user.getEmail(), user.getFullName());
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        sessionService.revokeAll(email);
        publish("password-changed", email, user.getFullName());
    }

    public User updateProfile(String email, String fullName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (fullName == null || fullName.isBlank()) {
            throw new RuntimeException("Name cannot be empty");
        }
        user.setFullName(fullName.trim());
        return userRepository.save(user);
    }

    public User setRole(String email, String role) {
        if (!role.equals("ROLE_USER") && !role.equals("ROLE_ADMIN")) {
            throw new RuntimeException("Role must be ROLE_USER or ROLE_ADMIN");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private String issueToken(String email, AuthToken.Type type, int hoursValid) {
        tokenRepository.deleteByEmailAndType(email, type);
        String raw = UUID.randomUUID() + "-" + UUID.randomUUID();
        AuthToken t = new AuthToken();
        t.setEmail(email);
        t.setTokenHash(sha256(raw));
        t.setType(type);
        t.setExpiresAt(LocalDateTime.now().plusHours(hoursValid));
        tokenRepository.save(t);
        return raw;
    }

    private void publish(String type, String email, String fullName) {
        kafkaTemplate.send(KafkaConfig.AUTH_EVENTS,
                new AuthEvent(type, email, fullName, LocalDateTime.now(), UUID.randomUUID().toString()));
    }

    static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
