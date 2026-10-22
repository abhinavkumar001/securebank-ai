package com.securebank.payee;

import com.securebank.common.CurrentUser;
import com.securebank.payee.dto.PayeeRequest;
import com.securebank.payee.dto.PayeeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeService payeeService;

    public PayeeController(PayeeService payeeService) {
        this.payeeService = payeeService;
    }

    @GetMapping
    public List<PayeeResponse> list(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return payeeService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayeeResponse add(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                             @Valid @RequestBody PayeeRequest request) {
        return payeeService.add(user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user, @PathVariable Long id) {
        payeeService.delete(user, id);
    }
}
