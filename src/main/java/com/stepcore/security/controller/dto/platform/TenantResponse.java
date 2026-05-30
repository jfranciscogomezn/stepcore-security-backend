package com.stepcore.security.controller.dto.platform;

import com.stepcore.security.domain.model.Tenant;

import java.time.LocalDateTime;

/**
 * Platform-facing view of a tenant, including its current user count against the cap.
 */
public record TenantResponse(
        Long id,
        String name,
        String slug,
        String plan,
        int maxUsers,
        long currentUsers,
        String status,
        boolean platform,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TenantResponse from(final Tenant tenant, final long currentUsers) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getPlan().name(),
                tenant.getMaxUsers(),
                currentUsers,
                tenant.getStatus().name(),
                tenant.isPlatform(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }
}
