package com.stepcore.security.controller.dto.platform;

public record PlatformUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String roleName,
        boolean enabled
) {}
