package com.kairos.backend.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Environment-specific settings. Startup fails fast on a missing or weak
 * signing key so the app can never run with a placeholder secret.
 */
@ConfigurationProperties(prefix = "kairos")
public record KairosProperties(String jwtSecret, Duration tokenTtl, String corsOrigins) {

    private static final int MIN_SECRET_BYTES = 32;

    public KairosProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "KAIROS_JWT_SECRET must be set to a random value of at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (tokenTtl == null || tokenTtl.isNegative() || tokenTtl.isZero()) {
            tokenTtl = Duration.ofHours(12);
        }
    }

    public byte[] jwtSecretBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public List<String> allowedOrigins() {
        if (corsOrigins == null || corsOrigins.isBlank()) return List.of();
        return Arrays.stream(corsOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
            .toList();
    }
}
