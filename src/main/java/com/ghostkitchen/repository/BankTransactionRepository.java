package com.ghostkitchen.repository;

import com.ghostkitchen.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    List<BankTransaction> findByBankStatement_IdOrderByTxnDateAsc(UUID bankStatementId);

    @Query("""
            SELECT bt FROM BankTransaction bt
            WHERE bt.organization.id = :organizationId
              AND bt.matched = false
              AND bt.txnDate BETWEEN :fromDate AND :toDate
            ORDER BY bt.txnDate ASC
            """)
    List<BankTransaction> findUnmatchedCandidates(@Param("organizationId") UUID organizationId,
                                                    @Param("fromDate") LocalDate fromDate,
                                                    @Param("toDate") LocalDate toDate);
}
