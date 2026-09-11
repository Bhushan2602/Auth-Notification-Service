package com.authplatform.session;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final JdbcTemplate jdbc;

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpired() {
        jdbc.update("DELETE FROM auth_tokens WHERE expires_at < NOW() - INTERVAL '7 days'");
        jdbc.update("DELETE FROM refresh_tokens WHERE expires_at < NOW() - INTERVAL '30 days'");
        jdbc.update("DELETE FROM login_audit WHERE created_at < NOW() - INTERVAL '90 days'");
    }
}
