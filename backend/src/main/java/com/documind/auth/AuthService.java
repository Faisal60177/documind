package com.documind.auth;

import com.documind.common.ApiException;
import com.documind.user.User;
import com.documind.user.UserRepository;
import com.documind.user.UserResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    /** Used to spend the same time on unknown emails, so login timing does not reveal accounts. */
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.dummyHash = passwordEncoder.encode("documind-timing-equalizer");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (exceedsBcryptLimit(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at most 72 bytes long");
        }
        String email = normalize(request.email());
        if (users.findByEmail(email).isPresent()) {
            throw emailTaken();
        }
        User user;
        try {
            user = users.saveAndFlush(new User(email, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException ex) {
            throw emailTaken();
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (exceedsBcryptLimit(request.password())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        Optional<User> found = users.findByEmail(normalize(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        return issueTokens(found.get());
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokens.consume(rawRefreshToken);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.createAccessToken(user),
                refreshTokens.issue(user),
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                refreshTokens.ttlSeconds(),
                UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "Email is already registered");
    }
}
