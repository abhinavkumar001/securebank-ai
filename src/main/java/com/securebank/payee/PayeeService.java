package com.securebank.payee;

import com.securebank.account.Account;
import com.securebank.account.AccountRepository;
import com.securebank.audit.AuditService;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import com.securebank.payee.dto.PayeeRequest;
import com.securebank.payee.dto.PayeeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PayeeService {

    private static final int MAX_PAYEES = 50;

    private final PayeeRepository payeeRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public PayeeService(PayeeRepository payeeRepository, AccountRepository accountRepository, AuditService auditService) {
        this.payeeRepository = payeeRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PayeeResponse> list(CurrentUser user) {
        return payeeRepository.findByUserIdOrderByNameAsc(user.id()).stream().map(PayeeResponse::from).toList();
    }

    @Transactional
    public PayeeResponse add(CurrentUser user, PayeeRequest request) {
        String number = request.accountNumber().trim().toUpperCase();
        Account account = accountRepository.findByAccountNumber(number)
                .orElseThrow(() -> ApiException.badRequest("No SecureBank account with number " + number));
        if (account.isOwnedBy(user.id())) {
            throw ApiException.badRequest("That's your own account. Use it directly in Move money.");
        }
        if (payeeRepository.existsByUserIdAndAccountNumber(user.id(), number)) {
            throw ApiException.conflict("This payee is already saved");
        }
        if (payeeRepository.findByUserIdOrderByNameAsc(user.id()).size() >= MAX_PAYEES) {
            throw ApiException.badRequest("You can save up to " + MAX_PAYEES + " payees");
        }
        Payee payee = payeeRepository.save(new Payee(user.id(), request.name().trim(), number));
        auditService.log(user.id(), "ADD_PAYEE", "Payee", payee.getId(), request.name().trim() + " · " + number);
        return PayeeResponse.from(payee);
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        Payee payee = payeeRepository.findById(id)
                .filter(p -> p.getUserId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Payee not found"));
        payeeRepository.delete(payee);
        auditService.log(user.id(), "DELETE_PAYEE", "Payee", id, payee.getName());
    }
}
