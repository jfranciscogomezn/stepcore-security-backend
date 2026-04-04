package com.gmm.devengos.service;

import com.gmm.devengos.controller.dto.role.CreateRoleRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionIdsRequest;
import com.gmm.devengos.controller.dto.role.RoleResponse;
import com.gmm.devengos.controller.dto.role.UpdateRoleRequest;
import com.gmm.devengos.controller.mapper.RoleMapper;
import com.gmm.devengos.domain.model.MenuOption;
import com.gmm.devengos.domain.model.Role;
import com.gmm.devengos.exception.RoleInUseException;
import com.gmm.devengos.exception.RoleNotFoundException;
import com.gmm.devengos.repository.MenuOptionRepository;
import com.gmm.devengos.repository.RoleRepository;
import com.gmm.devengos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private MenuOptionRepository menuOptionRepository;
    @Mock private RoleMapper roleMapper;

    @InjectMocks private RoleServiceImpl roleService;

    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .withId(1L)
                .withName("ADMIN")
                .withDescription("Admin role")
                .withMenuOptions(new HashSet<>())
                .build();
    }

    @Test
    void shouldReturnAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(adminRole));
        when(roleMapper.toRoleResponse(adminRole)).thenReturn(new RoleResponse(1L, "ADMIN", "Admin role", List.of()));

        final List<RoleResponse> result = roleService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("ADMIN");
    }

    @Test
    void shouldCreateRole() {
        final CreateRoleRequest request = new CreateRoleRequest("NEW_ROLE", "A new role");
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(java.util.Optional.empty());
        when(roleRepository.save(any())).thenReturn(adminRole);
        when(roleMapper.toRoleResponse(any())).thenReturn(new RoleResponse(1L, "NEW_ROLE", "A new role", List.of()));

        final RoleResponse result = roleService.create(request);

        assertThat(result.name()).isEqualTo("NEW_ROLE");
        verify(roleRepository).save(any());
    }

    @Test
    void shouldThrowRoleNotFoundWhenDeleteNonExistent() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleService.delete(99L)).isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void shouldThrowRoleInUseWhenDeletingRoleWithUsers() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByRoleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> roleService.delete(1L)).isInstanceOf(RoleInUseException.class);
    }

    @Test
    void shouldDeleteRoleWhenNoUsersAssigned() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByRoleId(1L)).thenReturn(false);

        roleService.delete(1L);

        verify(roleRepository).delete(adminRole);
    }

    @Test
    void shouldAssignMenuOptionsToRole() {
        final MenuOption opt = MenuOption.builder().withId(1L).withCode("MY_PROFILE")
                .withLabel("Profile").withRoute("/my/profile").withSortOrder(10).build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(menuOptionRepository.findAllById(List.of(1L))).thenReturn(List.of(opt));
        when(roleRepository.save(any())).thenReturn(adminRole);
        when(roleMapper.toRoleResponse(any())).thenReturn(new RoleResponse(1L, "ADMIN", null, List.of()));

        roleService.assignMenuOptions(1L, new MenuOptionIdsRequest(List.of(1L)));

        verify(roleRepository).save(adminRole);
    }

    @Test
    void shouldUpdateRoleDetails() {
        final UpdateRoleRequest request = new UpdateRoleRequest("UPDATED", "Updated desc");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(roleRepository.existsByNameAndIdNot("UPDATED", 1L)).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(adminRole);
        when(roleMapper.toRoleResponse(any())).thenReturn(new RoleResponse(1L, "UPDATED", "Updated desc", List.of()));

        final RoleResponse result = roleService.update(1L, request);

        assertThat(result.name()).isEqualTo("UPDATED");
    }
}
