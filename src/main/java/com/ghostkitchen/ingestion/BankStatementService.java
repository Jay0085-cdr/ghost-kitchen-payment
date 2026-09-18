package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.BankStatement;
import com.ghostkitchen.entity.BankTransaction;
import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.ReportStatus;
import com.ghostkitchen.exception.DuplicateUploadException;
import com.ghostkitchen.exception.InvalidRequestException;
import com.ghostkitchen.exception.ResourceNotFoundException;
import com.ghostkitchen.ingestion.adapter.ReportParseException;
import com.ghostkitchen.repository.BankStatementRepository;
import com.ghostkitchen.repository.BankTransactionRepository;
import com.ghostkitchen.repository.OrganizationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class BankStatementService {

    private final BankStatementRepository bankStatementRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final BankStatementParser parser;
    private final FileHashService fileHashService;

    public BankStatementService(BankStatementRepository bankStatementRepository,
                                 BankTransactionRepository bankTransactionRepository,
                                 OrganizationRepository organizationRepository,
                                 BankStatementParser parser,
                                 FileHashService fileHashService) {
        this.bankStatementRepository = bankStatementRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.organizationRepository = organizationRepository;
        this.parser = parser;
        this.fileHashService = fileHashService;
    }

    @Transactional
    public BankStatementResponse upload(UUID organizationId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty");
        }

        Organization organization = organizationRepository.getReferenceById(organizationId);
        byte[] content = readBytes(file);
        String fileHash = fileHashService.sha256Hex(content);

        bankStatementRepository.findByOrganization_IdAndFileHash(organizationId, fileHash)
                .ifPresent(existing -> {
                    throw new DuplicateUploadException("bank statement", existing.getId());
                });

        BankStatement statement = new BankStatement(organization, file.getOriginalFilename(), fileHash);

        try {
            List<ParsedBankLine> parsed = parser.parse(content);
            List<BankTransaction> transactions = parsed.stream()
                    .map(p -> toEntity(statement, organization, p))
                    .toList();

            statement.setStatus(ReportStatus.PARSED);
            bankStatementRepository.save(statement);
            bankTransactionRepository.saveAll(transactions);

            return BankStatementResponse.from(statement, transactions.stream().map(BankTransactionResponse::from).toList());
        } catch (ReportParseException e) {
            statement.setStatus(ReportStatus.FAILED);
            statement.setErrorDetail(e.getMessage());
            bankStatementRepository.save(statement);
            return BankStatementResponse.from(statement, List.of());
        }
    }

    public List<BankStatementResponse> listMine(UUID organizationId) {
        return bankStatementRepository.findByOrganization_IdOrderByUploadedAtDesc(organizationId).stream()
                .map(statement -> BankStatementResponse.from(statement, List.of()))
                .toList();
    }

    public BankStatementResponse getOwned(UUID statementId, UUID callerOrganizationId) {
        BankStatement statement = bankStatementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement not found: " + statementId));

        if (!statement.getOrganization().getId().equals(callerOrganizationId)) {
            throw new AccessDeniedException("Bank statement " + statementId + " is not accessible to this user");
        }

        List<BankTransactionResponse> transactions = bankTransactionRepository
                .findByBankStatement_IdOrderByTxnDateAsc(statementId).stream()
                .map(BankTransactionResponse::from)
                .toList();

        return BankStatementResponse.from(statement, transactions);
    }

    private BankTransaction toEntity(BankStatement statement, Organization organization, ParsedBankLine parsed) {
        BankTransaction transaction = new BankTransaction(statement, organization, parsed.txnDate(), parsed.amount());
        transaction.setNarration(parsed.narration());
        transaction.setReferenceNo(parsed.referenceNo());
        return transaction;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Could not read uploaded file: " + e.getMessage());
        }
    }
}
