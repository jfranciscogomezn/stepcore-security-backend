package com.stepcore.security.service;

import com.stepcore.security.controller.dto.role.CreateRoleRequest;
import com.stepcore.security.controller.dto.role.MenuNodeIdsRequest;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.controller.dto.role.UpdateRoleRequest;
import com.stepcore.security.controller.mapper.RoleMapper;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.exception.InvalidMenuNodeAssignmentException;
import com.stepcore.security.exception.RoleInUseException;
import com.stepcore.security.exception.RoleNotFoundException;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
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
    @Mock private MenuNodeRepository menuNodeRepository;
    @Mock private MenuTreeService menuTreeService;
    @Mock private RoleMapper roleMapper;

    @InjectMocks private RoleServiceImpl roleService;

    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .withId(1L)
                .withName("ADMIN")
                .withDescription("Admin role")
                .withMenuNodes(new HashSet<>())
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
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
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
    void shouldAssignItemMenuNodesToRole() {
        final MenuNode item = MenuNode.builder().withId(1L).withCode("MY_PROFILE")
                .withLabel("Profile").withRoute("/my/profile").withSortOrder(10)
                .withNodeType(MenuNodeType.ITEM).build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(menuNodeRepository.findAllById(List.of(1L))).thenReturn(List.of(item));
        when(roleRepository.save(any())).thenReturn(adminRole);
        when(roleMapper.toRoleResponse(any())).thenReturn(new RoleResponse(1L, "ADMIN", null, List.of()));

        roleService.assignMenuNodes(1L, new MenuNodeIdsRequest(List.of(1L)));

        verify(roleRepository).save(adminRole);
    }

    @Test
    void shouldRejectNonItemMenuNodeAssignment() {
        final MenuNode module = MenuNode.builder().withId(1L).withCode("PAYROLL")
                .withLabel("Payroll").withSortOrder(10).withNodeType(MenuNodeType.MODULE).build();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(menuNodeRepository.findAllById(List.of(1L))).thenReturn(List.of(module));

        assertThatThrownBy(() -> roleService.assignMenuNodes(1L, new MenuNodeIdsRequest(List.of(1L))))
                .isInstanceOf(InvalidMenuNodeAssignmentException.class);
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
