package com.securebank.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Runs inside the caller's transaction: if the business action rolls back,
     * its audit entry rolls back too, so the log never claims something happened that didn't.
     */
    public void log(Long userId, String action, String entityType, Object entityId, String details) {
        repository.save(new AuditLog(userId, action, entityType,
                entityId == null ? null : String.valueOf(entityId), details));
    }
}
