package com.stepcore.security.tenant;

import java.util.function.Supplier;

/**
 * Resolves the {@code tenantId} parameter of the auto-enabled Hibernate
 * {@code tenantFilter} from the request-scoped {@link TenantContext}.
 *
 * <p>Hibernate instantiates this resolver per session (via its managed-bean
 * registry) and calls {@link #get()} to obtain the current tenant id, so every
 * filtered query is automatically scoped to the active tenant. When no tenant is
 * set (system/seed operations, legacy single-tenant phase) it falls back to the
 * legacy tenant.</p>
 */
public class TenantIdFilterResolver implements Supplier<Long> {

    @Override
    public Long get() {
        return TenantContext.getTenantIdOrDefault();
    }
}
