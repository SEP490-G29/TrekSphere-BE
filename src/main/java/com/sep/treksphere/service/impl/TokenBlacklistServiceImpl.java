package com.sep.treksphere.service.impl;

import com.sep.treksphere.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set("blacklist:" + jti, "true", ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }
}
