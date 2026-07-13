package com.stepcore.security.service;

import com.stepcore.security.controller.dto.platform.PlatformUserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.exception.TenantNotFoundException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provider-plane operations over tenant users.
 * All repository calls use native queries to bypass the Hibernate tenantFilter,
 * allowing platform admin to operate cross-tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformUserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PlatformUserResponse> listByTenant(final Long tenantId) {
        assertTenantExists(tenantId);
        return userRepository.findUsersByTenantNative(tenantId).stream()
                .map(this::mapRow)
                .toList();
    }

    @Transactional
    public PlatformUserResponse setStatus(final Long tenantId, final Long userId,
                                          final UserStatusRequest request,
                                          final String actorEmail) {
        assertTenantExists(tenantId);
        final int updated = userRepository.setEnabledByIdAndTenantId(
                userId, tenantId, request.enabled());
        if (updated == 0) {
            throw new UserNotFoundException(userId);
        }
        auditService.logChange(
                actorEmail,
                "PLATFORM_SET_USER_STATUS",
                "USER",
                String.valueOf(userId),
                null,
                String.valueOf(request.enabled()),
                "platform admin recovery — tenantId=" + tenantId);
        log.info("[PlatformUserService] - SET_STATUS: userId={} tenantId={} enabled={} actor={}",
                userId, tenantId, request.enabled(), actorEmail);

        // Re-fetch to return current state
        return userRepository.findUsersByTenantNative(tenantId).stream()
                .map(this::mapRow)
                .filter(u -> u.id().equals(userId))
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void assertTenantExists(final Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private PlatformUserResponse mapRow(final Object[] row) {
        return new PlatformUserResponse(
                ((Number) row[0]).longValue(),   // id
                (String) row[1],                  // first_name
                (String) row[2],                  // last_name
                (String) row[3],                  // email
                (String) row[5],                  // role_name
                (Boolean) row[4]                  // enabled
        );
    }
}
