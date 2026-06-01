package com.stepcore.security.controller.dto.role;

import com.stepcore.security.domain.model.MenuNodeType;

import java.util.List;

public record MenuTreeNode(
        Long id,
        String code,
        String label,
        MenuNodeType type,
        String route,
        String icon,
        boolean enabled,
        List<MenuTreeNode> children
) {
    public MenuTreeNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
