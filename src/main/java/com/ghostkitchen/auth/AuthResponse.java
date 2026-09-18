package com.ghostkitchen.auth;

import com.ghostkitchen.entity.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UUID userId,
        UUID organizationId,
        String email,
        UserRole role
) {
}
