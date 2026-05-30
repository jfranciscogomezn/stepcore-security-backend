package com.stepcore.security.service;

import com.stepcore.security.controller.dto.auth.ChangePasswordRequest;
import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.auth.LoginResponse;
import com.stepcore.security.controller.mapper.RoleMapper;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.InvalidPasswordException;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private RoleMapper roleMapper;

    @InjectMocks private AuthServiceImpl authService;

    private User testUser;
    private Role testRole;

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
    }

    @Test
    void shouldReturnLoginResponseOnSuccessfulLogin() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(roleMapper.toMenuOptionResponse(any())).thenReturn(null);

        final LoginResponse response = authService.login(new LoginRequest("john@example.com", "Admin@1234!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.roleName()).isEqualTo("EMPLOYEE");
    }

    @Test
    void shouldThrowBadCredentialsWhenAuthManagerFails() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "wrong")))
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
