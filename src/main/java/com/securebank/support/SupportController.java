package com.securebank.support;

import com.securebank.common.CurrentUser;
import com.securebank.support.dto.ResolveRequest;
import com.securebank.support.dto.TicketRequest;
import com.securebank.support.dto.TicketResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/api/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse raise(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                @Valid @RequestBody TicketRequest request) {
        return supportService.raise(user, request.message());
    }

    @GetMapping("/api/tickets")
    public List<TicketResponse> mine(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return supportService.mine(user);
    }

    @GetMapping("/api/admin/tickets")
    public List<TicketResponse> queue(@RequestParam(defaultValue = "OPEN") String status) {
        return "RESOLVED".equalsIgnoreCase(status) ? supportService.resolved() : supportService.openQueue();
    }

    @PostMapping("/api/admin/tickets/{id}/resolve")
    public TicketResponse resolve(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser admin,
                                  @PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        return supportService.resolve(admin, id, request.resolution());
    }
}
