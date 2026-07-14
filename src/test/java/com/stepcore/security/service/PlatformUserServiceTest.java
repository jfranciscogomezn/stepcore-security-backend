package com.stepcore.security.service;

import com.stepcore.security.controller.dto.platform.PlatformUserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.exception.TenantNotFoundException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AuditService auditService;

    @InjectMocks private PlatformUserService platformUserService;

    private static final Long TENANT_ID = 3L;
    private static final Long USER_ID = 10L;

    private Object[] userRow(final Long id, final boolean enabled) {
        return new Object[]{id, "Ana", "Garcia", "ana@acme.qa", enabled, "ADMIN"};
    }

    @Test
    void shouldListUsersByTenant() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        final ArrayList<Object[]> rows = new ArrayList<>();
        rows.add(userRow(USER_ID, true));
        doReturn(rows).when(userRepository).findUsersByTenantNative(TENANT_ID);

        final List<PlatformUserResponse> result = platformUserService.listByTenant(TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(USER_ID);
        assertThat(result.get(0).email()).isEqualTo("ana@acme.qa");
        assertThat(result.get(0).enabled()).isTrue();
    }

    @Test
    void shouldThrowTenantNotFoundWhenListingUnknownTenant() {
        when(tenantRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> platformUserService.listByTenant(99L))
                .isInstanceOf(TenantNotFoundException.class);
        verify(userRepository, never()).findUsersByTenantNative(anyLong());
    }

    @Test
    void shouldReactivateUserSuccessfully() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(userRepository.setEnabledByIdAndTenantId(USER_ID, TENANT_ID, true)).thenReturn(1);
        final ArrayList<Object[]> rows = new ArrayList<>();
        rows.add(userRow(USER_ID, true));
        doReturn(rows).when(userRepository).findUsersByTenantNative(TENANT_ID);

        final PlatformUserResponse result = platformUserService.setStatus(
                TENANT_ID, USER_ID, new UserStatusRequest(true), "platform@stepcore.com");

        assertThat(result.enabled()).isTrue();
        verify(auditService).logChange(anyString(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldThrowUserNotFoundWhenUserNotInTenant() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(userRepository.setEnabledByIdAndTenantId(USER_ID, TENANT_ID, true)).thenReturn(0);

        assertThatThrownBy(() -> platformUserService.setStatus(
                TENANT_ID, USER_ID, new UserStatusRequest(true), "platform@stepcore.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
