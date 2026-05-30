package com.stepcore.security.config;

import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.tenant.TenantContext;
import com.stepcore.security.tenant.TenantGuc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private static final String ADMIN_EMAIL = "admin@stepcore.com";
    private static final String ADMIN_DEFAULT_PASSWORD = "Admin@2026!";

    private static final String PLATFORM_ADMIN_EMAIL = "platform@stepcore.com";
    private static final String PLATFORM_ADMIN_PASSWORD = "Platform@2026!";
    private static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantGuc tenantGuc;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultAdmin() {
        seedLegacyAdmin();
        seedPlatformAdmin();
    }

    private void seedLegacyAdmin() {
        try {
            TenantContext.setTenantId(TenantContext.LEGACY_TENANT_ID);
            tenantGuc.bind(TenantContext.LEGACY_TENANT_ID);
            if (userRepository.existsByEmail(ADMIN_EMAIL)) {
                return;
            }
            final Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            if (adminRole == null) {
                log.warn("[DataSeeder] - SEED: ADMIN role not found, skipping default admin creation");
                return;
            }
            final User admin = User.builder()
                    .withFirstName("System")
                    .withLastName("Administrator")
                    .withEmail(ADMIN_EMAIL)
                    .withPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD))
                    .withEnabled(true)
                    .withMustChangePassword(false)
                    .withRole(adminRole)
                    .withUpdatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("[DataSeeder] - SEED: default admin user created — email={} (change the password immediately!)", ADMIN_EMAIL);
        } finally {
            TenantContext.clear();
        }
    }

    private void seedPlatformAdmin() {
        try {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            tenantGuc.bind(TenantContext.PLATFORM_TENANT_ID);
            if (userRepository.existsByEmail(PLATFORM_ADMIN_EMAIL)) {
                return;
            }
            final Role platformRole = roleRepository.findByName(PLATFORM_ADMIN_ROLE).orElse(null);
            if (platformRole == null) {
                log.warn("[DataSeeder] - SEED: PLATFORM_ADMIN role not found, skipping platform admin creation");
                return;
            }
            final User platformAdmin = User.builder()
                    .withFirstName("Platform")
                    .withLastName("Operator")
                    .withEmail(PLATFORM_ADMIN_EMAIL)
                    .withPasswordHash(passwordEncoder.encode(PLATFORM_ADMIN_PASSWORD))
                    .withEnabled(true)
                    .withMustChangePassword(false)
                    .withRole(platformRole)
                    .withUpdatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(platformAdmin);
            log.info("[DataSeeder] - SEED: platform admin user created — email={} (change the password immediately!)", PLATFORM_ADMIN_EMAIL);
        } finally {
            TenantContext.clear();
        }
    }
}
