package com.securebank.transaction;

import com.securebank.common.CurrentUser;
import com.securebank.common.PageResponse;
import com.securebank.transaction.dto.AmountRequest;
import com.securebank.transaction.dto.TransactionResponse;
import com.securebank.transaction.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public TransactionResponse deposit(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                       @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key,
                                       @Valid @RequestBody AmountRequest request) {
        return transactionService.deposit(user, request, key);
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key,
                                        @Valid @RequestBody AmountRequest request) {
        return transactionService.withdraw(user, request, key);
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String key,
                                        @Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(user, request, key);
    }

    @GetMapping
    public PageResponse<TransactionResponse> statement(
            @RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.statement(user, accountNumber, page, size);
    }
}
