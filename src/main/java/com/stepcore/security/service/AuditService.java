package com.stepcore.security.service;

import com.stepcore.security.domain.model.AuditLog;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.AuditLogRepository;
import com.stepcore.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(
            final String actorEmail,
            final String action,
            final String entityType,
            final String entityId,
            final String oldValue,
            final String newValue,
            final String details) {

        final User actor = userRepository.findByEmail(actorEmail).orElse(null);

        final AuditLog entry = AuditLog.builder()
                .withUser(actor)
                .withAction(action)
                .withEntityType(entityType)
                .withEntityId(entityId)
                .withOldValue(oldValue)
                .withNewValue(newValue)
                .withDetails(details)
                .build();

        auditLogRepository.save(entry);
        log.info("[AuditService] - LOG: action={} entityType={} entityId={} actor={}", action, entityType, entityId, actorEmail);
    }
}
