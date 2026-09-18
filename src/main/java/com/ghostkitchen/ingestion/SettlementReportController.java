package com.ghostkitchen.ingestion;

import com.ghostkitchen.security.AppUserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlement-reports")
public class SettlementReportController {

    private final SettlementReportService settlementReportService;

    public SettlementReportController(SettlementReportService settlementReportService) {
        this.settlementReportService = settlementReportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SettlementReportResponse> upload(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam UUID platformId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestPart("file") MultipartFile file) {
        SettlementReportResponse response = settlementReportService.upload(
                principal.getOrganizationId(), platformId, periodStart, periodEnd, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public SettlementReportResponse get(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return settlementReportService.getOwned(id, principal.getOrganizationId());
    }

    @GetMapping
    public List<SettlementReportResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return settlementReportService.listMine(principal.getOrganizationId());
    }
}
