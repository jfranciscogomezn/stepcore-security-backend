package com.stepcore.security.controller.dto.user;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String roleName,
        Long roleId,
        boolean enabled,
        boolean mustChangePassword
) {}
