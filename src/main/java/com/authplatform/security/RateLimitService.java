package com.authplatform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final LoginAuditRepository auditRepository;

    @Value("${app.rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.rate-limit.lock-minutes:15}")
    private int lockMinutes;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int failures, LocalDateTime lockedUntil) {
    }

    public void checkAllowed(String email) {
        Attempt a = attempts.get(email.toLowerCase());
        if (a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked after too many attempts. Try again later.");
        }
    }

    public void recordSuccess(String email, String ip) {
        attempts.remove(email.toLowerCase());
        audit(email, true, "login ok", ip);
    }

    public void recordFailure(String email, String reason, String ip) {
        String key = email.toLowerCase();
        Attempt prev = attempts.getOrDefault(key, new Attempt(0, null));
        int failures = prev.failures() + 1;
        LocalDateTime lockedUntil = failures >= maxAttempts
                ? LocalDateTime.now().plusMinutes(lockMinutes) : null;
        attempts.put(key, new Attempt(failures, lockedUntil));
        audit(email, false, reason, ip);
    }

    private void audit(String email, boolean success, String reason, String ip) {
        LoginAudit entry = new LoginAudit();
        entry.setEmail(email);
        entry.setSuccess(success);
        entry.setReason(reason);
        entry.setIpAddress(ip);
        entry.setCreatedAt(LocalDateTime.now());
        auditRepository.save(entry);
    }
}
