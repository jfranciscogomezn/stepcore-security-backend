package com.stepcore.security.domain.model;

/**
 * Lifecycle status of a tenant. A SUSPENDED tenant cannot log in.
 */
public enum TenantStatus {
    PROVISIONING,
    ACTIVE,
    SUSPENDED
}
