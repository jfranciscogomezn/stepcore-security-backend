package com.stepcore.security.service;

import com.stepcore.security.controller.dto.user.CreateUserRequest;
import com.stepcore.security.controller.dto.user.UpdateUserRequest;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.controller.mapper.UserMapper;
import com.stepcore.security.domain.model.Role;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.AdminSelfDisableException;
import com.stepcore.security.exception.DuplicateEmailException;
import com.stepcore.security.exception.LastTenantAdminException;
import com.stepcore.security.exception.RoleNotFoundException;
import com.stepcore.security.exception.TenantNotFoundException;
import com.stepcore.security.exception.UserLimitReachedException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.RoleRepository;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(final Long id) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse create(final CreateUserRequest request, final String actorEmail) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        enforceUserLimit();
        final Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException(request.roleId()));

        final User user = User.builder()
                .withFirstName(request.firstName())
                .withLastName(request.lastName())
                .withEmail(request.email())
                .withPhone(request.phone())
                .withPasswordHash(passwordEncoder.encode(request.password()))
                .withEnabled(true)
                .withMustChangePassword(true)
                .withRole(role)
                .withUpdatedAt(LocalDateTime.now())
                .build();

        final User saved = userRepository.save(user);
        auditService.logChange(actorEmail, "CREATE_USER", "USER", String.valueOf(saved.getId()),
                null, request.email(), "User account created");
        log.info("[UserServiceImpl] - CREATE_USER: id={} email={}", saved.getId(), saved.getEmail());
        return userMapper.toUserResponse(saved);
    }

    @Override
    public UserResponse update(final Long id, final UpdateUserRequest request, final String actorEmail) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        if (userRepository.existsByEmailAndIdNot(user.getEmail(), id)) {
            throw new DuplicateEmailException(user.getEmail());
        }
        final Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException(request.roleId()));

        user.updateProfile(request.firstName(), request.lastName(), request.phone());
        user.updateRole(role);
        final User saved = userRepository.save(user);
        auditService.logChange(actorEmail, "UPDATE_USER", "USER", String.valueOf(id),
                null, null, "User profile updated");
        return userMapper.toUserResponse(saved);
    }

    @Override
    public void delete(final Long id, final String actorEmail) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
        auditService.logChange(actorEmail, "DELETE_USER", "USER", String.valueOf(id),
                user.getEmail(), null, "User account deleted");
        log.info("[UserServiceImpl] - DELETE_USER: id={}", id);
    }

    @Override
    public UserResponse setStatus(final Long id, final UserStatusRequest request, final String actorEmail) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!request.enabled()) {
            // Capa 1: block self-disable
            if (user.getEmail().equals(actorEmail)) {
                throw new AdminSelfDisableException();
            }
            // Capa 2: block disabling the last active admin of the tenant
            if ("ADMIN".equals(user.getRole().getName())) {
                final long remaining = userRepository.countActiveAdminsExcluding(
                        user.getTenantId(), user.getId());
                if (remaining == 0) {
                    throw new LastTenantAdminException();
                }
            }
        }

        final boolean previous = user.isEnabled();
        user.setEnabled(request.enabled());
        final User saved = userRepository.save(user);
        auditService.logChange(actorEmail, "SET_USER_STATUS", "USER", String.valueOf(id),
                String.valueOf(previous), String.valueOf(request.enabled()), null);
        return userMapper.toUserResponse(saved);
    }

    private void enforceUserLimit() {
        final Long tenantId = TenantContext.getTenantIdOrDefault();
        final Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (userRepository.countByTenantId(tenantId) >= tenant.getMaxUsers()) {
            throw new UserLimitReachedException(tenant.getMaxUsers());
        }
    }

    @Override
    public void resetPassword(final Long id, final String actorEmail) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        final String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "A1!";
        user.resetPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        auditService.logChange(actorEmail, "RESET_PASSWORD", "USER", String.valueOf(id),
                null, null, "Password reset by admin");
        log.info("[UserServiceImpl] - RESET_PASSWORD: userId={} actor={}", id, actorEmail);
    }
}
