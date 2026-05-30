package com.stepcore.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Makes the transaction advisor the outermost advice (order 0) so the tenant RLS aspect
 * ({@link com.stepcore.security.tenant.TenantRlsAspect}, order 10) runs <em>inside</em> an
 * active transaction and its {@code SET LOCAL app.current_tenant} takes effect.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TransactionConfig {
}
