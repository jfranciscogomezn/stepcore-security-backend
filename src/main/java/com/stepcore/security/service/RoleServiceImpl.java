package com.stepcore.security.service;

import com.stepcore.security.controller.dto.role.CreateRoleRequest;
import com.stepcore.security.controller.dto.role.MenuNodeIdsRequest;
import com.stepcore.security.controller.dto.role.MenuNodeResponse;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.controller.dto.role.UpdateRoleRequest;
import com.stepcore.security.controller.mapper.RoleMapper;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.exception.DuplicateEmailException;
import com.stepcore.security.exception.InvalidMenuNodeAssignmentException;
import com.stepcore.security.exception.RoleInUseException;
import com.stepcore.security.exception.RoleNotFoundException;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MenuNodeRepository menuNodeRepository;
    private final MenuTreeService menuTreeService;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(final Long id) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return roleMapper.toRoleResponse(role);
    }

    @Override
    public RoleResponse create(final CreateRoleRequest request) {
        if (roleRepository.findByName(request.name().toUpperCase()).isPresent()) {
            throw new DuplicateEmailException("Role name already exists: " + request.name());
        }
        final Role role = Role.builder()
                .withName(request.name().toUpperCase())
                .withDescription(request.description())
                .withMenuNodes(new HashSet<>())
                .build();
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    @Override
    public RoleResponse update(final Long id, final UpdateRoleRequest request) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        if (roleRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateEmailException("Role name already exists: " + request.name());
        }
        role.updateDetails(request.name().toUpperCase(), request.description());
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    @Override
    public void delete(final Long id) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        if (userRepository.existsByRoleId(id)) {
            throw new RoleInUseException(id);
        }
        roleRepository.delete(role);
        log.info("[RoleServiceImpl] - DELETE_ROLE: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeNode> getMenuCatalogue() {
        final List<MenuNode> catalogue = currentUserIsPlatformAdmin()
                ? menuNodeRepository.findAll()
                : menuNodeRepository.findByPlatformOnlyFalse();
        return menuTreeService.buildCatalogueTree(catalogue, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuNodeResponse> getAssignedMenuNodes(final Long id) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return roleMapper.sortedMenuNodes(role);
    }

    @Override
    public RoleResponse assignMenuNodes(final Long id, final MenuNodeIdsRequest request) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        final List<MenuNode> nodes = menuNodeRepository.findAllById(request.menuNodeIds());
        if (nodes.size() != request.menuNodeIds().size()) {
            throw new InvalidMenuNodeAssignmentException("One or more menu nodes were not found");
        }
        final boolean hasNonItem = nodes.stream().anyMatch(node -> node.getNodeType() != MenuNodeType.ITEM);
        if (hasNonItem) {
            throw new InvalidMenuNodeAssignmentException("Only ITEM menu nodes can be assigned to a role");
        }
        if (!currentUserIsPlatformAdmin() && nodes.stream().anyMatch(MenuNode::isPlatformOnly)) {
            throw new InvalidMenuNodeAssignmentException("Platform-only menu nodes cannot be assigned to tenant roles");
        }
        role.replaceMenuNodes(new HashSet<>(nodes));
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    private static boolean currentUserIsPlatformAdmin() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PLATFORM_ADMIN".equals(a.getAuthority()));
    }
}
