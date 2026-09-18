package com.ghostkitchen.organization;

import com.ghostkitchen.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/me")
    public OrganizationResponse getMyOrganization(@AuthenticationPrincipal AppUserPrincipal principal) {
        return organizationService.getOrganization(principal.getOrganizationId(), principal.getOrganizationId());
    }

    /**
     * Proves org-scoping enforcement end-to-end: {id} is client-supplied, but the service
     * rejects it (403) unless it matches the caller's own organization from the JWT.
     */
    @GetMapping("/{id}")
    public OrganizationResponse getOrganization(@PathVariable UUID id,
                                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        return organizationService.getOrganization(id, principal.getOrganizationId());
    }
}
