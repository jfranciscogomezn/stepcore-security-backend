package com.stepcore.security.repository;

import com.stepcore.security.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByRoleId(Long roleId);

    /**
     * Tenant-scoped user count for plan-limit enforcement and platform reporting.
     * Native query so it is NOT subject to the auto-enabled Hibernate {@code tenantFilter}
     * (which would otherwise restrict the count to the request's own tenant).
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE tenant_id = :tenantId", nativeQuery = true)
    long countByTenantId(@Param("tenantId") Long tenantId);
}
