package com.stepcore.security.exception;

public class LastTenantAdminException extends RuntimeException {
    public LastTenantAdminException() {
        super("Cannot disable the last active admin of a tenant");
    }
}
