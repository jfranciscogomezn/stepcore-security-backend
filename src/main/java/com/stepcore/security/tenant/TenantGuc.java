package com.stepcore.security.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Binds the current tenant to the PostgreSQL session variable {@code app.current_tenant},
 * which the Row-Level Security policies read. Uses {@code set_config(..., is_local => true)}
 * so the value is scoped to the active transaction and reset automatically on commit/rollback
 * (safe with connection pooling).
 */
@Component
public class TenantGuc {

    static final String SETTING = "app.current_tenant";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Applies {@code tenantId} as the transaction-local RLS tenant. No-op when no transaction
     * is active (SET LOCAL requires a transaction) or when {@code tenantId} is null.
     */
    public void bind(final Long tenantId) {
        if (tenantId == null || !TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        entityManager
                .createNativeQuery("SELECT set_config(:name, :value, true)")
                .setParameter("name", SETTING)
                .setParameter("value", String.valueOf(tenantId))
                .getSingleResult();
    }
}
