package com.ghostkitchen.repository;

import com.ghostkitchen.entity.PlatformTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlatformTransactionRepository extends JpaRepository<PlatformTransaction, UUID> {

    List<PlatformTransaction> findBySettlementReport_IdOrderByOrderDateAsc(UUID settlementReportId);

    long countBySettlementReport_Id(UUID settlementReportId);

    @Query("""
            SELECT pt FROM PlatformTransaction pt
            WHERE pt.organization.id = :organizationId
              AND pt.settlementReport.platform.id = :platformId
              AND pt.settlementReport.periodStart >= :periodStart
              AND pt.settlementReport.periodEnd <= :periodEnd
            ORDER BY pt.orderDate ASC
            """)
    List<PlatformTransaction> findForReconciliation(@Param("organizationId") UUID organizationId,
                                                      @Param("platformId") UUID platformId,
                                                      @Param("periodStart") LocalDate periodStart,
                                                      @Param("periodEnd") LocalDate periodEnd);
}
