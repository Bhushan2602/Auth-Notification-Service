package com.authplatform.security;

import com.authplatform.auth.User;
import com.authplatform.auth.UserRepository;
import com.authplatform.notification.AuthEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(name == null ? email : name);
            u.setEmail(email);
            u.setProvider("GOOGLE");
            u.setEmailVerified(true);
            u.setRole("ROLE_USER");
            u.setCreatedAt(LocalDateTime.now());
            User saved = userRepository.save(u);
            kafkaTemplate.send("auth-events", new AuthEvent("user-registered", email, name, LocalDateTime.now(), UUID.randomUUID().toString()));
            return saved;
        });

        // Frontend exchanges this one-time token via the cookie-less redirect
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole());
        response.sendRedirect("/oauth-success?token=" + accessToken + "&email=" + user.getEmail());
    }
}
