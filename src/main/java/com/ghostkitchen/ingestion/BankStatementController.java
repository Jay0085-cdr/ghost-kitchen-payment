package com.ghostkitchen.ingestion;

import com.ghostkitchen.security.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank-statements")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    public BankStatementController(BankStatementService bankStatementService) {
        this.bankStatementService = bankStatementService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BankStatementResponse> upload(@AuthenticationPrincipal AppUserPrincipal principal,
                                                          @RequestPart("file") MultipartFile file) {
        BankStatementResponse response = bankStatementService.upload(principal.getOrganizationId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public BankStatementResponse get(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return bankStatementService.getOwned(id, principal.getOrganizationId());
    }

    @GetMapping
    public List<BankStatementResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return bankStatementService.listMine(principal.getOrganizationId());
    }
}
