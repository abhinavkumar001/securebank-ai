package com.securebank.audit;

import com.securebank.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only: the JwtInterceptor blocks /api/admin/** for customers. */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public PageResponse<AuditLog> list(@RequestParam(required = false) Long userId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.from(userId == null
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }
}
