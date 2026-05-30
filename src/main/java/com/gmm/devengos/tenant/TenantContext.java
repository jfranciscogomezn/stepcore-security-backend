package com.gmm.devengos.tenant;

import java.util.UUID;

/**
 * Request-scoped holder of the current tenant id, propagated via a {@link ThreadLocal}.
 *
 * <p>The current tenant is resolved per request from the authenticated JWT claim and
 * cleared at the end of the request. System operations that run outside any request
 * (data seeding, the legacy single-tenant phase) fall back to {@link #LEGACY_TENANT_ID}
 * through {@link #getTenantIdOrDefault()}.</p>
 */
public final class TenantContext {

    /** Reserved tenant that owns all pre-multi-tenancy (Phase-1) data. */
    public static final UUID LEGACY_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Reserved tenant for the SaaS provider plane (PLATFORM_ADMIN operators). */
    public static final UUID PLATFORM_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(final UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * @return the current tenant id, or the legacy tenant id when none is set.
     */
    public static UUID getTenantIdOrDefault() {
        final UUID tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : LEGACY_TENANT_ID;
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
