package com.ghostkitchen.organization;

import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.exception.ResourceNotFoundException;
import com.ghostkitchen.repository.OrganizationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * requestedId is client-supplied; callerOrganizationId comes from the caller's JWT.
     * Demonstrates the tenant-isolation rule from ARCHITECTURE.md Section 8: organization_id
     * scoping is always derived server-side, and any client-supplied id must be checked
     * against it, never trusted on its own.
     */
    public OrganizationResponse getOrganization(UUID requestedId, UUID callerOrganizationId) {
        if (!requestedId.equals(callerOrganizationId)) {
            throw new AccessDeniedException("Organization " + requestedId + " is not accessible to this user");
        }

        Organization organization = organizationRepository.findById(requestedId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + requestedId));

        return OrganizationResponse.from(organization);
    }
}
