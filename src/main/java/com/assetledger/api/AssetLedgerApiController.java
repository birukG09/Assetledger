package com.assetledger.api;

import com.assetledger.domain.AssetType;
import com.assetledger.domain.Token;
import com.assetledger.domain.Transaction;
import com.assetledger.service.AssetLedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public final class AssetLedgerApiController {
    private final AssetLedgerService service;

    public AssetLedgerApiController(AssetLedgerService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                service.ledger().validateIntegrity() ? "UP" : "DEGRADED",
                service.ledger().validateIntegrity()
        );
    }

    @GetMapping("/summary")
    public SummaryResponse summary() {
        BigDecimal totalValue = service.tokens().stream()
                .map(token -> token.asset().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int ownerCount = service.registry().currentOwners().values().stream()
                .map(owner -> owner.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet())
                .size();
        return new SummaryResponse(
                service.registry().size(),
                totalValue,
                service.ledger().size(),
                ownerCount,
                service.ledger().validateIntegrity()
        );
    }

    @GetMapping("/assets")
    public List<AssetResponse> assets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AssetType type
    ) {
        String normalizedSearch = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);
        return service.tokens().stream()
                .filter(token -> type == null || token.asset().type() == type)
                .filter(token -> normalizedSearch.isBlank() || matches(token, normalizedSearch))
                .map(token -> toAssetResponse(token))
                .toList();
    }

    @GetMapping("/assets/{tokenId}")
    public AssetResponse asset(@PathVariable String tokenId) {
        return toAssetResponse(findToken(tokenId));
    }

    @PostMapping("/assets")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse registerAsset(@Valid @RequestBody CreateAssetRequest request)
            throws Exception {
        Token token = service.registerAsset(
                request.type(),
                request.value(),
                request.owner(),
                request.metadata() == null ? java.util.Map.of() : request.metadata()
        );
        return toAssetResponse(token);
    }

    @GetMapping("/ledger/transactions")
    public List<TransactionResponse> transactions() {
        return service.transactions().stream()
                .map(AssetLedgerApiController::toTransactionResponse)
                .toList();
    }

    @GetMapping("/ledger/verify")
    public HealthResponse verifyLedger() {
        boolean valid = service.ledger().validateIntegrity();
        return new HealthResponse(valid ? "VALID" : "INVALID", valid);
    }

    @PostMapping("/assets/{tokenId}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @PathVariable String tokenId,
            @Valid @RequestBody TransferRequest request
    ) throws Exception {
        findToken(tokenId);
        return toTransactionResponse(service.transfer(tokenId, request.recipient()));
    }

    private Token findToken(String tokenId) {
        Token token = service.registry().getToken(tokenId);
        if (token == null) {
            throw new NoSuchElementException("Token not found: " + tokenId);
        }
        return token;
    }

    private AssetResponse toAssetResponse(Token token) {
        return new AssetResponse(
                token.tokenId(),
                token.asset().id(),
                token.asset().type(),
                token.asset().value(),
                token.asset().owner(),
                service.registry().currentOwner(token.tokenId()),
                token.asset().metadata(),
                token.createdAt()
        );
    }

    private boolean matches(Token token, String search) {
        String currentOwner = service.registry().currentOwner(token.tokenId());
        String metadata = token.asset().metadata().toString();
        return List.of(
                        token.tokenId(),
                        token.asset().id(),
                        token.asset().owner(),
                        currentOwner,
                        token.asset().type().name(),
                        metadata
                ).stream()
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(search));
    }

    private static TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.transactionId(),
                transaction.fromOwner(),
                transaction.toOwner(),
                transaction.tokenId(),
                transaction.timestamp(),
                transaction.previousHash(),
                transaction.hash()
        );
    }
}