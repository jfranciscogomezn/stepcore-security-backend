package com.stepcore.security.tenant;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Binds the request's tenant to the RLS session variable at the start of every transactional
 * unit of work. Ordered to run <em>inside</em> the transaction advisor (which is configured at
 * order 0 in {@link com.stepcore.security.config.TransactionConfig}), so the {@code SET LOCAL}
 * issued by {@link TenantGuc} applies to the active transaction.
 *
 * <p>Flows that switch tenants mid-transaction (login, provisioning, seeding) re-bind explicitly
 * via {@link TenantGuc} after changing {@link TenantContext}.</p>
 */
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class TenantRlsAspect {

    private final TenantGuc tenantGuc;

    @Around("@within(org.springframework.transaction.annotation.Transactional) "
            + "|| @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object bindTenant(final ProceedingJoinPoint joinPoint) throws Throwable {
        tenantGuc.bind(TenantContext.getTenantIdOrDefault());
        return joinPoint.proceed();
    }
}
