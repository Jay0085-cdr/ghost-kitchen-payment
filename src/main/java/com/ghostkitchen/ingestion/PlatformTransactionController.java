package com.ghostkitchen.ingestion;

import com.ghostkitchen.repository.PlatformTransactionRepository;
import com.ghostkitchen.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform-transactions")
public class PlatformTransactionController {

    private final SettlementReportService settlementReportService;
    private final PlatformTransactionRepository platformTransactionRepository;

    public PlatformTransactionController(SettlementReportService settlementReportService,
                                          PlatformTransactionRepository platformTransactionRepository) {
        this.settlementReportService = settlementReportService;
        this.platformTransactionRepository = platformTransactionRepository;
    }

    @GetMapping
    public List<PlatformTransactionResponse> list(@RequestParam UUID reportId,
                                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        settlementReportService.assertOwned(reportId, principal.getOrganizationId());
        return platformTransactionRepository.findBySettlementReport_IdOrderByOrderDateAsc(reportId).stream()
                .map(PlatformTransactionResponse::from)
                .toList();
    }
}
