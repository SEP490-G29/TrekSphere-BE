package com.sep.treksphere.service.impl;

import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationRateLimiterImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationRateLimiterImpl rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new EmailVerificationRateLimiterImpl(redisTemplate);
        ReflectionTestUtils.setField(rateLimiter, "cooldownSeconds", 60L);
        ReflectionTestUtils.setField(rateLimiter, "maxPerHour", 5L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkAllowed_AllowsRequestAndStartsHourlyWindow() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimiter.checkAllowed("user@example.com");

        verify(redisTemplate).expire(anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    void checkAllowed_RejectsRequestDuringCooldown() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(false);

        assertThatThrownBy(() -> rateLimiter.checkAllowed("user@example.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_RESEND_RATE_LIMITED);

        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void checkAllowed_RejectsMoreThanFiveRequestsPerHour() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(6L);

        assertThatThrownBy(() -> rateLimiter.checkAllowed("user@example.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_RESEND_RATE_LIMITED);
    }
}
