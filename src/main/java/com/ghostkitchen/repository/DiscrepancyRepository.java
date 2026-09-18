package com.ghostkitchen.repository;

import com.ghostkitchen.entity.Discrepancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, UUID> {

    List<Discrepancy> findByReconciliationResult_Id(UUID reconciliationResultId);
}
