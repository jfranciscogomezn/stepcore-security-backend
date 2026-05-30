package com.stepcore.security.exception;

public class TenantSlugAlreadyExistsException extends RuntimeException {

    public TenantSlugAlreadyExistsException(final String slug) {
        super("Tenant slug already exists: " + slug);
    }
}
