package com.liuhecai.auth;

import com.liuhecai.common.enums.AuthRealm;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
public class JwtService {

    private static final String FORBIDDEN_SECRET = "liuhecai-dev-secret-change-me-32bytes!!";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String RANDOM_SECRET_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecretKey key;
    private final long expireSeconds;

    public JwtService(
            @Value("${liuhecai.jwt.secret:}") String secret,
            @Value("${liuhecai.jwt.expire-seconds:86400}") long expireSeconds,
            Environment environment) {
        this.expireSeconds = expireSeconds;
        this.key = Keys.hmacShaKeyFor(resolveSecret(secret, environment).getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(AuthUser user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("realm", user.getRealm().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key);
        if (user.getTenantId() != null) {
            builder.claim("tenantId", user.getTenantId());
        }
        builder.claim("tv", user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        return builder.compact();
    }

    public AuthUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            AuthRealm realm = AuthRealm.valueOf(claims.get("realm", String.class));
            Long tenantId = claims.get("tenantId", Long.class);
            Integer tv = claims.get("tv", Integer.class);
            if (tv == null) {
                tv = 0;
            }
            return new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    realm,
                    tenantId,
                    tv
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static String resolveSecret(String secret, Environment environment) {
        if (isInvalidSecret(secret)) {
            if (isLocalProfile(environment)) {
                String generated = generateRandomSecret(48);
                log.error(
                        "JWT secret is missing or invalid in local profile; using ephemeral random secret "
                                + "(tokens invalid after restart). Set liuhecai.jwt.secret in application-local.yml.");
                return generated;
            }
            throw new IllegalStateException(
                    "liuhecai.jwt.secret must be set, at least 32 characters, and must not use the forbidden default");
        }
        return secret;
    }

    private static boolean isInvalidSecret(String secret) {
        return secret == null
                || secret.isBlank()
                || secret.length() < MIN_SECRET_LENGTH
                || FORBIDDEN_SECRET.equals(secret);
    }

    private static boolean isLocalProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equals);
    }

    private static String generateRandomSecret(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_SECRET_ALPHABET.charAt(random.nextInt(RANDOM_SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }
}
