package com.ahogeking.studentanalytics.common;

import com.ahogeking.studentanalytics.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtUtil {
    private String secret;
    private long expirationHours = 6;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret 未配置");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 长度不能小于 32 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createLoginToken(Integer userId, String username, String realName, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("realName", realName);
        claims.put("role", role);
        return createToken(String.valueOf(userId), claims);
    }

    public String createToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiresAt = new Date(
                now.getTime() + Duration.ofHours(expirationHours).toMillis()
        );

        return Jwts.builder()
                .subject(subject)
                .claims(new HashMap<>(claims))
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        String realToken = resolveToken(token);
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(realToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("登录状态已失效，请重新登录", e);
        }
    }

    public Integer getUserId(String token) {
        Claims claims = parseToken(token);
        return Integer.valueOf(claims.getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String getRealName(String token) {
        return parseToken(token).get("realName", String.class);
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    private String resolveToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException("未登录");
        }
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
