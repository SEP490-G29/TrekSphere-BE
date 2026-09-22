package com.sep.treksphere.auth;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private String getKey(String userEmail) {
        return "rt:active:" + userEmail;
    }

    public void store(String userEmail, String jti, long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(getKey(userEmail), jti, ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void validateAndConsume(String userEmail, String jti) {
        String activeJti = redisTemplate.opsForValue().get(getKey(userEmail));

        if (activeJti == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Không tìm thấy phiên đăng nhập hợp lệ");
        }

        if (!activeJti.equals(jti)) {
            log.warn("The old refresh token is being reused for the user. {}", userEmail);
            revokeAll(userEmail);
            throw new AppException(ErrorCode.INVALID_TOKEN, "Phát hiện truy cập bất thường, vui lòng đăng nhập lại");
        }
    }

    public void revokeAll(String userEmail) {
        redisTemplate.delete(getKey(userEmail));
    }
}
