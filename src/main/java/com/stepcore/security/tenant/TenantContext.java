package com.stepcore.security.tenant;

/**
 * Request-scoped holder of the current tenant id, propagated via a {@link ThreadLocal}.
 *
 * <p>The current tenant is resolved per request from the authenticated JWT claim and
 * cleared at the end of the request. System operations that run outside any request
 * (data seeding, the legacy single-tenant phase) fall back to {@link #LEGACY_TENANT_ID}
 * through {@link #getTenantIdOrDefault()}.</p>
 */
public final class TenantContext {

    /** Reserved tenant for the SaaS provider plane (PLATFORM_ADMIN operators). */
    public static final Long PLATFORM_TENANT_ID = 1L;

    /** Reserved tenant that owns all pre-multi-tenancy (Phase-1) data. */
    public static final Long LEGACY_TENANT_ID = 2L;

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(final Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * @return the current tenant id, or the legacy tenant id when none is set.
     */
    public static Long getTenantIdOrDefault() {
        final Long tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : LEGACY_TENANT_ID;
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
