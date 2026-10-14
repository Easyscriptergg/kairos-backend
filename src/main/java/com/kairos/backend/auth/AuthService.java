package com.kairos.backend.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kairos.backend.auth.AuthDtos.AuthResponse;
import com.kairos.backend.auth.AuthDtos.UserDto;
import com.kairos.backend.common.ApiException;
import com.kairos.backend.config.KairosProperties;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "The email or password is incorrect.";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final KairosProperties properties;
    /** Compared against when the email is unknown, so both failures take the same time. */
    private final String dummyHash;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, KairosProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(String email, String password) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(normalized).isPresent()) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        UserAccount user = users.saveAndFlush(new UserAccount(normalized, passwordEncoder.encode(password)));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return users.findById(userId).map(UserDto::of).orElseThrow(() -> ApiException.unauthorized("This account no longer exists."));
    }

    private AuthResponse issueToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("kairos-backend")
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, expiresAt, UserDto.of(user));
    }
}
