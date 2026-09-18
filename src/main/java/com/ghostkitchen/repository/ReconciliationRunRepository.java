package com.ghostkitchen.repository;

import com.ghostkitchen.entity.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

    List<ReconciliationRun> findByOrganization_IdOrderByExecutedAtDesc(UUID organizationId);
}
