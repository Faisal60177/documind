package com.documind.auth;

import com.documind.common.ApiException;
import com.documind.config.JwtProperties;
import com.documind.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Creates a new refresh token and returns the raw value. Only its hash is stored. */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
        repository.save(new RefreshToken(user, hash(raw), expiresAt));
        return raw;
    }

    /** Validates a refresh token, revokes it (rotation) and returns its owner. */
    @Transactional
    public User consume(String rawToken) {
        Instant now = Instant.now();
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .filter(t -> t.isActive(now))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        token.revoke(now);
        return token.getUser();
    }

    /** Revokes a refresh token if it exists. Safe to call repeatedly. */
    @Transactional
    public void revoke(String rawToken) {
        Instant now = Instant.now();
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(now));
    }

    public long ttlSeconds() {
        return properties.refreshTokenTtl().toSeconds();
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
