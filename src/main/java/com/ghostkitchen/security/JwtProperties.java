package com.ghostkitchen.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

@ConfigurationProperties(prefix = "ghost-kitchen.jwt")
public class JwtProperties {

    private String secret;
    private long expirationMs = 86_400_000L;

    @PostConstruct
    void validate() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "ghost-kitchen.jwt.secret (JWT_SECRET env var) must be set and at least 32 bytes long for HS256");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
