package com.sep.treksphere.security;

import com.sep.treksphere.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${application.security.jwt.reset-password.expiration}")
    private long resetPasswordExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.getId());
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration, "access");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration, "refresh");
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration, String type) {
        extraClaims.put("type", type);
        return Jwts
                .builder()
                .claims().add(extraClaims).and()
                .subject(userDetails.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isSignatureValid(String token) {
        try {
            Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public String generatePasswordResetToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        String pwdRev = UUID.nameUUIDFromBytes(user.getPasswordHash().getBytes()).toString();
        extraClaims.put("pwd_rev", pwdRev);
        
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + resetPasswordExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean validatePasswordResetToken(String token, User user) {
        final String username = extractUsername(token);
        if (!username.equals(user.getEmail()) || isTokenExpired(token)) {
            return false;
        }
        
        String tokenPwdRev = extractClaim(token, claims -> claims.get("pwd_rev", String.class));
        String currentPwdRev = UUID.nameUUIDFromBytes(user.getPasswordHash().getBytes()).toString();
        
        return currentPwdRev.equals(tokenPwdRev);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
