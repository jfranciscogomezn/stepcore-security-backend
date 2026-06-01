package com.stepcore.security.controller.dto.menu;

import com.stepcore.security.domain.model.MenuNodeType;

public record MenuNodeAdminResponse(
        Long id,
        String code,
        String label,
        MenuNodeType nodeType,
        String route,
        String icon,
        Long parentId,
        int sortOrder,
        boolean enabled
) {}
