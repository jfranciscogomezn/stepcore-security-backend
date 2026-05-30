package com.stepcore.security.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import com.stepcore.security.tenant.TenantIdFilterResolver;

import java.time.LocalDateTime;

/**
 * A client company using the SaaS platform. Tenants are global (not tenant-scoped):
 * every other tenant-owned entity references a tenant via its {@code tenant_id}.
 *
 * <p>Defines the auto-enabled {@code tenantFilter}: Hibernate applies it to every
 * session and resolves its {@code tenantId} parameter from {@link TenantContext}
 * via {@link TenantIdFilterResolver}, so tenant-owned entities are transparently
 * scoped to the current tenant on queries.</p>
 */
@Entity
@Table(name = "tenants")
@FilterDef(
        name = "tenantFilter",
        autoEnabled = true,
        parameters = @ParamDef(name = "tenantId", type = Long.class, resolver = TenantIdFilterResolver.class)
)
@Getter
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantPlan plan = TenantPlan.STANDARD;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private int maxUsers = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "is_platform", nullable = false)
    @Builder.Default
    private boolean platform = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == TenantStatus.SUSPENDED;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePlan(final TenantPlan newPlan, final int newMaxUsers) {
        this.plan = newPlan;
        this.maxUsers = newMaxUsers;
        this.updatedAt = LocalDateTime.now();
    }
}
