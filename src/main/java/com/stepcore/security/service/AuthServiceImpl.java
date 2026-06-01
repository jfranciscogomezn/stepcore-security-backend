package com.stepcore.security.service;

import com.stepcore.security.controller.dto.auth.ChangePasswordRequest;
import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.auth.LoginResponse;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.mapper.UserMapper;
import com.stepcore.security.domain.model.MenuNode;
import com.stepcore.security.domain.model.MenuNodeType;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.InvalidPasswordException;
import com.stepcore.security.exception.TenantSuspendedException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.MenuNodeRepository;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.security.JwtService;
import com.stepcore.security.tenant.TenantContext;
import com.stepcore.security.tenant.TenantGuc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final MenuNodeRepository menuNodeRepository;
    private final MenuTreeService menuTreeService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final TenantGuc tenantGuc;

    @Override
    public LoginResponse login(final LoginRequest request) {
        final Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(() -> new BadCredentialsException("Invalid tenant or credentials"));

        if (!tenant.isActive()) {
            throw new TenantSuspendedException(tenant.getSlug());
        }

        try {
            TenantContext.setTenantId(tenant.getId());
            tenantGuc.bind(tenant.getId());

            final User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new BadCredentialsException("Invalid tenant or credentials"));

            if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid tenant or credentials");
            }

            final UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
                    .build();

            final List<MenuNode> catalogue = menuNodeRepository.findAll();
            final List<MenuNode> assignedItems = user.getRole().getMenuNodes().stream()
                    .filter(node -> node.getNodeType() == MenuNodeType.ITEM)
                    .toList();
            final List<MenuTreeNode> menu = menuTreeService.buildTree(assignedItems, catalogue);
            final List<String> permissions = menuTreeService.extractPermissions(assignedItems);

            final Map<String, Object> claims = Map.of(
                    JwtService.CLAIM_TENANT_ID, tenant.getId().toString(),
                    JwtService.CLAIM_TENANT_SLUG, tenant.getSlug(),
                    JwtService.CLAIM_TENANT_PLAN, tenant.getPlan().name(),
                    JwtService.CLAIM_ROLES, List.of(user.getRole().getName()),
                    JwtService.CLAIM_PERMISSIONS, permissions);

            final String token = jwtService.generateToken(userDetails, claims);

            log.info("[AuthServiceImpl] - LOGIN: tenant={} user={} role={}",
                    tenant.getSlug(), user.getEmail(), user.getRole().getName());

            return new LoginResponse(token, user.getEmail(), user.getFullName(),
                    user.getRole().getName(), menu, permissions, user.isMustChangePassword(),
                    tenant.getSlug(), tenant.getName(), tenant.getPlan().name());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(final String email) {
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toUserResponse(user);
    }

    @Override
    public void changePassword(final String email, final ChangePasswordRequest request) {
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        final String newHash = passwordEncoder.encode(request.newPassword());
        user.updatePassword(newHash);
        userRepository.save(user);

        auditService.logChange(email, "CHANGE_PASSWORD", "USER", String.valueOf(user.getId()),
                null, null, "User changed own password");

        log.info("[AuthServiceImpl] - CHANGE_PASSWORD: user={}", email);
    }
}
