package com.stepcore.security.controller.dto.platform;

/**
 * Result of provisioning a tenant. The temporary admin password is returned <strong>once</strong>
 * so the provider can hand it over; the admin must change it on first login.
 */
public record ProvisionTenantResponse(
        TenantResponse tenant,
        String adminEmail,
        String temporaryPassword
) {}
