package com.stepcore.security.controller.dto.auth;

import com.stepcore.security.controller.dto.role.MenuOptionResponse;

import java.util.List;

public record LoginResponse(
        String token,
        String email,
        String fullName,
        String roleName,
        List<MenuOptionResponse> menuOptions,
        boolean mustChangePassword
) {}
