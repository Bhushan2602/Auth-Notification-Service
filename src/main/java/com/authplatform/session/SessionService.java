package com.authplatform.session;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-days:7}")
    private long refreshDays;

    public String issue(String email, String deviceLabel) {
        String raw = UUID.randomUUID() + "-" + UUID.randomUUID();
        RefreshToken t = new RefreshToken();
        t.setEmail(email);
        t.setTokenHash(sha256(raw));
        t.setDeviceLabel(deviceLabel == null ? "unknown device" : deviceLabel);
        t.setExpiresAt(LocalDateTime.now().plusDays(refreshDays));
        refreshTokenRepository.save(t);
        return raw;
    }

    public String rotate(String rawRefreshToken) {        RefreshToken t = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (t.isRevoked() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        t.setRevoked(true);
        refreshTokenRepository.save(t);
        return issue(t.getEmail(), t.getDeviceLabel());
    }

    public String ownerOf(String rawRefreshToken) {
        return refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .map(RefreshToken::getEmail)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public List<RefreshToken> sessions(String email) {
        return refreshTokenRepository.findByEmailAndRevokedFalse(email);
    }

    public void revokeAll(String email) {
        refreshTokenRepository.findByEmailAndRevokedFalse(email).forEach(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    public void revokeOne(Long id, String email) {
        RefreshToken t = refreshTokenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!t.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Forbidden: not your session");
        }
        t.setRevoked(true);
        refreshTokenRepository.save(t);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
