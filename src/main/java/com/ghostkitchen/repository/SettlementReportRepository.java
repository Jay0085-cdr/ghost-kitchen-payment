package com.ghostkitchen.repository;

import com.ghostkitchen.entity.SettlementReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementReportRepository extends JpaRepository<SettlementReport, UUID> {

    Optional<SettlementReport> findByOrganization_IdAndPlatform_IdAndPeriodStartAndPeriodEndAndFileHash(
            UUID organizationId, UUID platformId, LocalDate periodStart, LocalDate periodEnd, String fileHash);

    List<SettlementReport> findByOrganization_IdOrderByUploadedAtDesc(UUID organizationId);
}
