package com.ghostkitchen.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Shared reference data (Swiggy, Zomato, Direct, ...) — intentionally has no organization. */
@Entity
@Table(name = "platform")
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Platform() {
    }

    public Platform(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
