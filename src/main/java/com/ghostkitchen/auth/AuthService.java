package com.ghostkitchen.auth;

import com.ghostkitchen.entity.AppUser;
import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.UserRole;
import com.ghostkitchen.exception.DuplicateEmailException;
import com.ghostkitchen.repository.AppUserRepository;
import com.ghostkitchen.repository.OrganizationRepository;
import com.ghostkitchen.security.AppUserPrincipal;
import com.ghostkitchen.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(OrganizationRepository organizationRepository,
                        AppUserRepository appUserRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Self-registration creates a brand-new organization with this user as its OWNER —
     * there is no "join an existing organization" flow yet (no invite mechanism exists),
     * so every registration is a new tenant's first user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Organization organization = organizationRepository.save(new Organization(request.organizationName()));

        AppUser user = new AppUser(
                organization, request.email(), passwordEncoder.encode(request.password()), UserRole.OWNER);
        appUserRepository.save(user);

        return issueToken(AppUserPrincipal.fromEntity(user));
    }

    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        return issueToken((AppUserPrincipal) authentication.getPrincipal());
    }

    private AuthResponse issueToken(AppUserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        return new AuthResponse(
                token, "Bearer", jwtService.getExpirationMs(),
                principal.getUserId(), principal.getOrganizationId(), principal.getUsername(), principal.getRole());
    }
}
