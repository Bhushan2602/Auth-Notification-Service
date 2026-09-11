package com.authplatform.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock LoginAuditRepository auditRepository;

    private RateLimitService service(int max, int lockMin) {
        RateLimitService s = new RateLimitService(auditRepository);
        ReflectionTestUtils.setField(s, "maxAttempts", max);
        ReflectionTestUtils.setField(s, "lockMinutes", lockMin);
        return s;
    }

    @Test
    void locksAfterMaxFailures_andReleasesAfterSuccess() {
        RateLimitService s = service(3, 15);
        s.recordFailure("a@x.com", "bad", "127.0.0.1");
        s.recordFailure("a@x.com", "bad", "127.0.0.1");
        assertThatNoException().isThrownBy(() -> s.checkAllowed("a@x.com"));
        s.recordFailure("a@x.com", "bad", "127.0.0.1");
        assertThatThrownBy(() -> s.checkAllowed("a@x.com"))
                .hasMessageContaining("locked");
        s.recordSuccess("a@x.com", "127.0.0.1");
        assertThatNoException().isThrownBy(() -> s.checkAllowed("a@x.com"));
    }
}
