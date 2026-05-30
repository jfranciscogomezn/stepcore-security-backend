package com.stepcore.security.config;

import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultAdmin() {
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
    }
}
