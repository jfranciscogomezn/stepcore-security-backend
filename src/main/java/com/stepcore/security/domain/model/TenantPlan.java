package com.stepcore.security.domain.model;

/**
 * Subscription plan of a tenant. Drives the default user cap and feature limits.
 */
public enum TenantPlan {
    STANDARD(50),
    PREMIUM(100);

    private final int defaultMaxUsers;

    TenantPlan(final int defaultMaxUsers) {
        this.defaultMaxUsers = defaultMaxUsers;
    }

    /** Default user cap when a tenant does not override {@code max_users}. */
    public int defaultMaxUsers() {
        return defaultMaxUsers;
    }
}
