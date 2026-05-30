package com.stepcore.security.service;

import com.stepcore.security.controller.dto.auth.ChangePasswordRequest;
import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.auth.LoginResponse;
import com.stepcore.security.controller.mapper.RoleMapper;
import com.stepcore.security.controller.mapper.UserMapper;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.TenantPlan;
import com.stepcore.security.domain.model.TenantStatus;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.InvalidPasswordException;
import com.stepcore.security.exception.TenantSuspendedException;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private RoleMapper roleMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthServiceImpl authService;

    private static final String TENANT_SLUG = "legacy";

    private User testUser;
    private Role testRole;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testRole = Role.builder()
                .withId(1L)
                .withName("EMPLOYEE")
                .withMenuOptions(new HashSet<>())
                .build();
        testUser = User.builder()
                .withId(1L)
                .withFirstName("John")
                .withLastName("Doe")
                .withEmail("john@example.com")
                .withPasswordHash("$2a$12$hashed")
                .withEnabled(true)
                .withMustChangePassword(false)
                .withRole(testRole)
                .build();
        testTenant = Tenant.builder()
                .withId(UUID.randomUUID())
                .withName("Legacy")
                .withSlug(TENANT_SLUG)
                .withPlan(TenantPlan.STANDARD)
                .withStatus(TenantStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldReturnLoginResponseOnSuccessfulLogin() {
        when(tenantRepository.findBySlug(TENANT_SLUG)).thenReturn(Optional.of(testTenant));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Admin@1234!", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        final LoginResponse response =
                authService.login(new LoginRequest(TENANT_SLUG, "john@example.com", "Admin@1234!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.roleName()).isEqualTo("EMPLOYEE");
        assertThat(response.tenantSlug()).isEqualTo(TENANT_SLUG);
        assertThat(response.tenantPlan()).isEqualTo("STANDARD");
    }

    @Test
    void shouldThrowBadCredentialsWhenTenantIsUnknown() {
        when(tenantRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "john@example.com", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldThrowTenantSuspendedWhenTenantIsNotActive() {
        testTenant = Tenant.builder()
                .withId(UUID.randomUUID()).withName("Legacy").withSlug(TENANT_SLUG)
                .withPlan(TenantPlan.STANDARD).withStatus(TenantStatus.SUSPENDED).build();
        when(tenantRepository.findBySlug(TENANT_SLUG)).thenReturn(Optional.of(testTenant));

        assertThatThrownBy(() -> authService.login(new LoginRequest(TENANT_SLUG, "john@example.com", "x")))
                .isInstanceOf(TenantSuspendedException.class);
    }

    @Test
    void shouldThrowBadCredentialsWhenPasswordIsWrong() {
        when(tenantRepository.findBySlug(TENANT_SLUG)).thenReturn(Optional.of(testTenant));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(TENANT_SLUG, "john@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordIsCorrect() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Admin@1234!", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NewPass@2026!")).thenReturn("$2a$12$newhash");

        authService.changePassword("john@example.com",
                new ChangePasswordRequest("Admin@1234!", "NewPass@2026!"));

        verify(userRepository).save(testUser);
        verify(auditService).logChange(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldThrowInvalidPasswordWhenCurrentPasswordIsWrong() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("john@example.com",
                new ChangePasswordRequest("wrong", "NewPass@2026!")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Current password is incorrect");
    }
}
