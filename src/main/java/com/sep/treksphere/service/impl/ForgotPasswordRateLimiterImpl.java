package com.sep.treksphere.service.impl;

import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.service.ForgotPasswordRateLimiter;
import lombok.RequiredArgsConstructor;
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
public class ForgotPasswordRateLimiterImpl implements ForgotPasswordRateLimiter {

    private static final String KEY_PREFIX = "forgot-password:rate-limit:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void checkAllowed(String email) {
        String emailHash = hash(email.trim().toLowerCase(Locale.ROOT));
        String cooldownKey = KEY_PREFIX + "cooldown:" + emailHash;

        Boolean cooldownCreated = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey,
                "1",
                Duration.ofMinutes(5)
        );
        if (!Boolean.TRUE.equals(cooldownCreated)) {
            throw new AppException(ErrorCode.FORGOT_PASSWORD_RATE_LIMITED);
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
