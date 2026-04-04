package com.gmm.devengos.service;

import com.gmm.devengos.controller.dto.role.CreateRoleRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionIdsRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionResponse;
import com.gmm.devengos.controller.dto.role.RoleResponse;
import com.gmm.devengos.controller.dto.role.UpdateRoleRequest;
import com.gmm.devengos.controller.mapper.RoleMapper;
import com.gmm.devengos.domain.model.MenuOption;
import com.gmm.devengos.domain.model.Role;
import com.gmm.devengos.exception.DuplicateEmailException;
import com.gmm.devengos.exception.RoleInUseException;
import com.gmm.devengos.exception.RoleNotFoundException;
import com.gmm.devengos.repository.MenuOptionRepository;
import com.gmm.devengos.repository.RoleRepository;
import com.gmm.devengos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
    private final MenuOptionRepository menuOptionRepository;
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
                .withMenuOptions(new HashSet<>())
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
    public List<MenuOptionResponse> getMenuOptions(final Long id) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return role.getMenuOptions().stream()
                .sorted(Comparator.comparingInt(MenuOption::getSortOrder))
                .map(roleMapper::toMenuOptionResponse)
                .toList();
    }

    @Override
    public RoleResponse assignMenuOptions(final Long id, final MenuOptionIdsRequest request) {
        final Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        final Set<MenuOption> options = new HashSet<>(
                menuOptionRepository.findAllById(request.menuOptionIds()));
        role.replaceMenuOptions(options);
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }
}
