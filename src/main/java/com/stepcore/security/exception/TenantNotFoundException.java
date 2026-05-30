package com.stepcore.security.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(final Long id) {
        super("Tenant not found: " + id);
    }
}
