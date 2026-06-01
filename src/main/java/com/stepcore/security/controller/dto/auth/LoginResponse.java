package com.stepcore.security.controller.dto.auth;

import com.stepcore.security.controller.dto.role.MenuTreeNode;

import java.util.List;

public record LoginResponse(
        String token,
        String email,
        String fullName,
        String roleName,
        List<MenuTreeNode> menu,
        List<String> permissions,
        boolean mustChangePassword,
        String tenantSlug,
        String tenantName,
        String tenantPlan
) {}
