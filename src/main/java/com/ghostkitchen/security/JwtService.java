package com.ghostkitchen.security;

import com.ghostkitchen.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_ORGANIZATION_ID = "organizationId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getUserId().toString())
                .claim(CLAIM_ORGANIZATION_ID, principal.getOrganizationId().toString())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .claim(CLAIM_EMAIL, principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.getExpirationMs())))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return properties.getExpirationMs();
    }

    /** Returns empty on any invalid/expired/tampered token instead of throwing, for the filter to handle uniformly. */
    public Optional<AppUserPrincipal> parseToPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            UUID organizationId = UUID.fromString(claims.get(CLAIM_ORGANIZATION_ID, String.class));
            UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
            String email = claims.get(CLAIM_EMAIL, String.class);

            return Optional.of(AppUserPrincipal.fromJwtClaims(userId, organizationId, email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
