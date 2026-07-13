package com.stepcore.security.repository;

import com.stepcore.security.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByRoleId(Long roleId);

    /**
     * Tenant-scoped user count for plan-limit enforcement and platform reporting.
     * Native query — NOT subject to the Hibernate tenantFilter.
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Count active ADMIN-role users in a tenant, excluding one specific user.
     * Used by the last-admin guard in setStatus().
     * Native query — bypasses tenantFilter so it works regardless of TenantContext.
     */
    @Query(value = """
            SELECT COUNT(*) FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.tenant_id = :tenantId
              AND r.name = 'ADMIN'
              AND u.enabled = true
              AND u.id != :excludeUserId
            """, nativeQuery = true)
    long countActiveAdminsExcluding(@Param("tenantId") Long tenantId,
                                    @Param("excludeUserId") Long excludeUserId);

    /**
     * List users of a specific tenant for platform-admin recovery.
     * Native query — bypasses tenantFilter so platform admin (tenantId=1) can
     * see users of any other tenant.
     */
    @Query(value = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.enabled, r.name AS role_name
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.tenant_id = :tenantId
            ORDER BY u.last_name, u.first_name
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> findUsersByTenantNative(@Param("tenantId") Long tenantId);

    /**
     * Update the enabled flag of a specific user within a tenant.
     * Native query — allows platform admin to operate cross-tenant.
     * Returns the number of rows updated (0 = user not found in that tenant).
     */
    @Modifying
    @Query(value = "UPDATE users SET enabled = :enabled WHERE id = :userId AND tenant_id = :tenantId",
           nativeQuery = true)
    int setEnabledByIdAndTenantId(@Param("userId") Long userId,
                                   @Param("tenantId") Long tenantId,
                                   @Param("enabled") boolean enabled);
}
