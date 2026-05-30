package com.stepcore.security.controller.dto.platform;

import com.stepcore.security.domain.model.TenantPlan;
import com.stepcore.security.domain.model.TenantStatus;
import jakarta.validation.constraints.Positive;

/**
 * Partial update of a tenant. All fields are optional; only the supplied ones are applied.
 * {@code status} can move a tenant between ACTIVE and SUSPENDED (PROVISIONING is internal).
 */
public record UpdateTenantRequest(
        TenantPlan plan,

        @Positive(message = "maxUsers must be a positive number")
        Integer maxUsers,

        TenantStatus status
) {}
