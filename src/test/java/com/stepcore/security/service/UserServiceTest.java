package com.stepcore.security.service;

import com.stepcore.security.controller.dto.user.CreateUserRequest;
import com.stepcore.security.controller.dto.user.UpdateUserRequest;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.controller.mapper.UserMapper;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.TenantPlan;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.DuplicateEmailException;
import com.stepcore.security.exception.RoleNotFoundException;
import com.stepcore.security.exception.UserHasAssociatedRecordsException;
import com.stepcore.security.exception.UserLimitReachedException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    private Role employeeRole;
    private User testUser;
    private UserResponse testUserResponse;
    private Tenant currentTenant;

    @BeforeEach
    void setUp() {
        employeeRole = Role.builder()
                .withId(2L).withName("EMPLOYEE").withMenuOptions(new HashSet<>()).build();
        testUser = User.builder()
                .withId(1L).withFirstName("Ana").withLastName("Garcia")
                .withEmail("ana@example.com").withPasswordHash("$2a$12$hash")
                .withEnabled(true).withMustChangePassword(true).withRole(employeeRole).build();
        testUserResponse = new UserResponse(1L, "Ana", "Garcia", "ana@example.com",
                null, "EMPLOYEE", 2L, true, true);
        currentTenant = Tenant.builder()
                .withId(TenantContext.LEGACY_TENANT_ID).withName("Legacy").withSlug("legacy")
                .withPlan(TenantPlan.PREMIUM).withMaxUsers(50).build();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        final CreateUserRequest request = new CreateUserRequest(
                "Ana", "Garcia", "ana@example.com", null, "Admin@1234!", 2L);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(tenantRepository.findById(TenantContext.LEGACY_TENANT_ID)).thenReturn(Optional.of(currentTenant));
        when(userRepository.countByTenantId(TenantContext.LEGACY_TENANT_ID)).thenReturn(3L);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("Admin@1234!")).thenReturn("$2a$12$hash");
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        final UserResponse result = userService.create(request, "admin@example.com");

        assertThat(result.email()).isEqualTo("ana@example.com");
        verify(auditService).logChange(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldThrowDuplicateEmailWhenEmailAlreadyExists() {
        final CreateUserRequest request = new CreateUserRequest(
                "Ana", "Garcia", "ana@example.com", null, "Admin@1234!", 2L);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request, "admin@example.com"))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowRoleNotFoundWhenRoleDoesNotExist() {
        final CreateUserRequest request = new CreateUserRequest(
                "Ana", "Garcia", "ana@example.com", null, "Admin@1234!", 99L);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(tenantRepository.findById(TenantContext.LEGACY_TENANT_ID)).thenReturn(Optional.of(currentTenant));
        when(userRepository.countByTenantId(TenantContext.LEGACY_TENANT_ID)).thenReturn(3L);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request, "admin@example.com"))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void shouldThrowUserLimitReachedWhenCapExceeded() {
        final CreateUserRequest request = new CreateUserRequest(
                "Ana", "Garcia", "ana@example.com", null, "Admin@1234!", 2L);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(tenantRepository.findById(TenantContext.LEGACY_TENANT_ID)).thenReturn(Optional.of(currentTenant));
        when(userRepository.countByTenantId(TenantContext.LEGACY_TENANT_ID)).thenReturn(50L);

        assertThatThrownBy(() -> userService.create(request, "admin@example.com"))
                .isInstanceOf(UserLimitReachedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldEnableAndDisableUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        userService.setStatus(1L, new UserStatusRequest(false), "admin@example.com");

        verify(userRepository).save(testUser);
        verify(auditService).logChange(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldResetPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$resetHash");
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.resetPassword(1L, "admin@example.com");

        verify(userRepository).save(testUser);
        verify(auditService).logChange(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.delete(1L, "admin@example.com");

        verify(userRepository).delete(testUser);
    }

    @Test
    void shouldUpdateUserProfile() {
        final UpdateUserRequest request = new UpdateUserRequest("Updated", "Name", "555-1234", 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndIdNot(any(), any())).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(employeeRole));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        final UserResponse result = userService.update(1L, request, "admin@example.com");

        assertThat(result).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldThrowUserNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        final List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(1);
    }
}
