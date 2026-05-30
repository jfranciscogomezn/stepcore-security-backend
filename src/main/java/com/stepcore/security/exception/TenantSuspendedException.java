package com.stepcore.security.exception;

/**
 * Raised when authentication is attempted against a tenant that is not active
 * (e.g. SUSPENDED). Mapped to HTTP 403.
 */
public class TenantSuspendedException extends RuntimeException {

    public TenantSuspendedException(final String tenantSlug) {
        super("Tenant is not active: " + tenantSlug);
    }
}
