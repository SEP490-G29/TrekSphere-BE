package com.sep.treksphere.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${application.cookie.secure:false}")
    private boolean secure;

    @Value("${application.cookie.same-site:Strict}")
    private String sameSite;

    public static String extract(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String extractAccessToken(HttpServletRequest request) {
        String token = extract(request, "access_token");
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        return token;
    }

    public ResponseCookie build(String name, String value, long maxAgeSecs, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAgeSecs)
                .build();
    }

    public ResponseCookie expire(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
    }

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public HttpHeaders createCookieHeaders(String accessToken, String refreshToken) {
       HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, build("access_token", accessToken, jwtExpiration / 1000, "/").toString());
        headers.add(HttpHeaders.SET_COOKIE, build("refresh_token", refreshToken, refreshExpiration / 1000, "/api/v1/auth").toString());
        return headers;
    }

    public HttpHeaders createExpiredCookieHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expire("access_token", "/").toString());
        headers.add(HttpHeaders.SET_COOKIE, expire("refresh_token", "/api/v1/auth").toString());
        return headers;
    }
}
