package com.gmm.devengos.controller.dto.auth;

import com.gmm.devengos.controller.dto.role.MenuOptionResponse;

import java.util.List;

public record LoginResponse(
        String token,
        String email,
        String fullName,
        String roleName,
        List<MenuOptionResponse> menuOptions,
        boolean mustChangePassword
) {}
