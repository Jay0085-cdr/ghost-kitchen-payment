package com.ghostkitchen.repository;

import com.ghostkitchen.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {

    Optional<BankStatement> findByOrganization_IdAndFileHash(UUID organizationId, String fileHash);

    List<BankStatement> findByOrganization_IdOrderByUploadedAtDesc(UUID organizationId);
}
