package com.stepcore.security.service;

import com.stepcore.security.controller.dto.auth.ChangePasswordRequest;
import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.auth.LoginResponse;
import com.stepcore.security.controller.dto.role.MenuOptionResponse;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.mapper.RoleMapper;
import com.stepcore.security.controller.mapper.UserMapper;
import com.stepcore.security.domain.model.User;
import com.stepcore.security.exception.InvalidPasswordException;
import com.stepcore.security.exception.UserNotFoundException;
import com.stepcore.security.repository.UserRepository;
import com.stepcore.security.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserDetailsService userDetailsService;

    @Override
    public LoginResponse login(final LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        final String token = jwtService.generateToken(userDetails);

        final List<MenuOptionResponse> menuOptions = user.getRole().getMenuOptions().stream()
                .sorted(Comparator.comparingInt(opt -> opt.getSortOrder()))
                .map(roleMapper::toMenuOptionResponse)
                .toList();

        log.info("[AuthServiceImpl] - LOGIN: user={} role={}", user.getEmail(), user.getRole().getName());

        return new LoginResponse(token, user.getEmail(), user.getFullName(),
                user.getRole().getName(), menuOptions, user.isMustChangePassword());
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
