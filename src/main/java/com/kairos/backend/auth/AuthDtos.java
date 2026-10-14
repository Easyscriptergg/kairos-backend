package com.kairos.backend.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 128, message = "must be between 10 and 128 characters") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserDto(UUID id, String email) {
        static UserDto of(UserAccount user) {
            return new UserDto(user.getId(), user.getEmail());
        }
    }

    public record AuthResponse(String token, Instant expiresAt, UserDto user) {
    }
}
