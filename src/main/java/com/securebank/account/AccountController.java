package com.securebank.account;

import com.securebank.account.dto.AccountResponse;
import com.securebank.account.dto.OpenAccountRequest;
import com.securebank.account.dto.UpdateStatusRequest;
import com.securebank.common.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/api/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                @Valid @RequestBody OpenAccountRequest request) {
        return accountService.open(user, request);
    }

    @GetMapping("/api/accounts")
    public List<AccountResponse> listMine(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return accountService.listMine(user);
    }

    @GetMapping("/api/accounts/{accountNumber}")
    public AccountResponse get(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                               @PathVariable String accountNumber) {
        return accountService.get(user, accountNumber);
    }

    // ----- Admin -----

    @GetMapping("/api/admin/accounts")
    public List<AccountResponse> listAll() {
        return accountService.listAll();
    }

    @PatchMapping("/api/admin/accounts/{accountNumber}/status")
    public AccountResponse updateStatus(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser admin,
                                        @PathVariable String accountNumber,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return accountService.updateStatus(admin, accountNumber, request);
    }
}
