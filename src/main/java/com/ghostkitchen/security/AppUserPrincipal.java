package com.ghostkitchen.security;

import com.ghostkitchen.entity.AppUser;
import com.ghostkitchen.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated principal. Built from the DB at login time (CustomUserDetailsService,
 * needs the password hash) and rebuilt directly from validated JWT claims on every
 * subsequent request (JwtAuthenticationFilter, no DB round trip, no password needed).
 */
public class AppUserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;

    public AppUserPrincipal(UUID userId, UUID organizationId, String email, String passwordHash, UserRole role) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static AppUserPrincipal fromEntity(AppUser user) {
        return new AppUserPrincipal(
                user.getId(), user.getOrganization().getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
    }

    public static AppUserPrincipal fromJwtClaims(UUID userId, UUID organizationId, String email, UserRole role) {
        return new AppUserPrincipal(userId, organizationId, email, "", role);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
