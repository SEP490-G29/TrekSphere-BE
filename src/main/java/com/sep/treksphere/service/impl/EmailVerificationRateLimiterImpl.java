package com.sep.treksphere.service.impl;

import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.service.EmailVerificationRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationRateLimiterImpl implements EmailVerificationRateLimiter {

    private static final String KEY_PREFIX = "email-verification:rate-limit:";

    private final StringRedisTemplate redisTemplate;

    @Value("${application.email-verification.rate-limit.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${application.email-verification.rate-limit.max-per-hour:5}")
    private long maxPerHour;

    @Override
    public void checkAllowed(String email) {
        String emailHash = hash(email.trim().toLowerCase(Locale.ROOT));
        String cooldownKey = KEY_PREFIX + "cooldown:" + emailHash;

        Boolean cooldownCreated = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey,
                "1",
                Duration.ofSeconds(cooldownSeconds)
        );
        if (!Boolean.TRUE.equals(cooldownCreated)) {
            throw new AppException(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED);
        }

        String hourlyKey = KEY_PREFIX + "hourly:" + emailHash;
        Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);
        if (hourlyCount != null && hourlyCount == 1L) {
            redisTemplate.expire(hourlyKey, Duration.ofHours(1));
        }
        if (hourlyCount == null || hourlyCount > maxPerHour) {
            throw new AppException(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
