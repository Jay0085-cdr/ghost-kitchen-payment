package com.ghostkitchen.repository;

import com.ghostkitchen.entity.ReconciliationResult;
import com.ghostkitchen.entity.ReconciliationResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    List<ReconciliationResult> findByReconciliationRun_IdOrderByCreatedAtAsc(UUID reconciliationRunId);

    List<ReconciliationResult> findByReconciliationRun_IdAndStatusOrderByCreatedAtAsc(
            UUID reconciliationRunId, ReconciliationResultStatus status);

    long countByReconciliationRun_IdAndStatus(UUID reconciliationRunId, ReconciliationResultStatus status);

    /** Prevents a later run from re-matching an order that a prior run already resolved. */
    @Query("""
            SELECT rr.platformTransaction.id FROM ReconciliationResult rr
            WHERE rr.organization.id = :organizationId AND rr.platformTransaction IS NOT NULL
            """)
    List<UUID> findReconciledPlatformTransactionIds(@Param("organizationId") UUID organizationId);
}
