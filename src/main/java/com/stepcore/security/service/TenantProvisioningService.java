package com.stepcore.security.service;

import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.tenant.TenantContext;
import com.stepcore.security.tenant.TenantGuc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds the baseline a new tenant needs to operate: an {@code ADMIN} role with full menu
 * access and an initial administrator account. Runs inside the caller's transaction so a
 * failure rolls the whole tenant back. While seeding, the {@link TenantContext} is switched
 * to the target tenant so {@code @PrePersist} stamps the correct {@code tenant_id} and the
 * tenant filter scopes lookups; the previous context is always restored.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private static final String TENANT_ADMIN_ROLE = "ADMIN";
    private static final Set<String> PLATFORM_ONLY_ITEM_CODES = Set.of("PLATFORM_TENANTS");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MenuNodeRepository menuNodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantGuc tenantGuc;

    /**
     * @return the one-time temporary password generated for the initial admin.
     */
    public String provisionInitialAdmin(final Tenant tenant, final String adminEmail,
                                        final String adminFirstName, final String adminLastName) {
        final Long previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenant.getId());
            tenantGuc.bind(tenant.getId());

            final Set<MenuNode> tenantMenuItems = menuNodeRepository.findAllByNodeTypeOrderBySortOrderAsc(MenuNodeType.ITEM)
                    .stream()
                    .filter(node -> !PLATFORM_ONLY_ITEM_CODES.contains(node.getCode()))
                    .collect(Collectors.toSet());

            final Role adminRole = roleRepository.save(Role.builder()
                    .withName(TENANT_ADMIN_ROLE)
                    .withDescription("Tenant administrator")
                    .withMenuNodes(new HashSet<>(tenantMenuItems))
                    .build());

            final String temporaryPassword = generateTemporaryPassword();
            final User admin = userRepository.save(User.builder()
                    .withFirstName(adminFirstName)
                    .withLastName(adminLastName)
                    .withEmail(adminEmail)
                    .withPasswordHash(passwordEncoder.encode(temporaryPassword))
                    .withEnabled(true)
                    .withMustChangePassword(true)
                    .withRole(adminRole)
                    .withUpdatedAt(LocalDateTime.now())
                    .build());

            log.info("[TenantProvisioningService] - PROVISION: tenant={} adminUserId={} adminEmail={}",
                    tenant.getSlug(), admin.getId(), adminEmail);
            return temporaryPassword;
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenantId(previousTenant);
                tenantGuc.bind(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "A1!";
    }
}
