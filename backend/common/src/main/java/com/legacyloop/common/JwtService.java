package com.legacyloop.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and reads access tokens. One class for both directions: user-service signs, all three
 * services verify with the same secret.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;
    private final String issuer;

    public JwtService(@Value("${legacyloop.jwt.secret}") String secret,
                      @Value("${legacyloop.jwt.access-token-seconds:3600}") long accessTokenSeconds,
                      @Value("${legacyloop.jwt.refresh-token-seconds:604800}") long refreshTokenSeconds,
                      @Value("${legacyloop.jwt.issuer:legacyloop}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.issuer = issuer;
    }

    public long accessTokenSeconds() {
        return accessTokenSeconds;
    }

    public long refreshTokenSeconds() {
        return refreshTokenSeconds;
    }

    public String issueAccessToken(Long userId, String email, String fullName, Set<String> roles,
                                   Long institutionId, boolean premium) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenSeconds * 1000))
                .claims(Map.of(
                        "email", email,
                        "name", fullName == null ? "" : fullName,
                        "roles", List.copyOf(roles),
                        "institutionId", institutionId == null ? -1L : institutionId,
                        "premium", premium))
                .signWith(key)
                .compact();
    }

    /** @return the caller described by the token, or null when it is invalid or expired. */
    @SuppressWarnings("unchecked")
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            long institutionId = claims.get("institutionId", Number.class).longValue();
            return new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("name", String.class),
                    new HashSet<>(claims.get("roles", List.class)),
                    institutionId < 0 ? null : institutionId,
                    Boolean.TRUE.equals(claims.get("premium", Boolean.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
