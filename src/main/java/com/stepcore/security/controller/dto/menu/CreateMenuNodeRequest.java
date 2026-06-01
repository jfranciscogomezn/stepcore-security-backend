package com.stepcore.security.controller.dto.menu;

import com.stepcore.security.domain.model.MenuNodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMenuNodeRequest(
        @NotBlank(message = "Code is required")
        @Size(max = 100, message = "Code must not exceed 100 characters")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be uppercase letters, digits, or underscores")
        String code,

        @NotBlank(message = "Label is required")
        @Size(max = 150, message = "Label must not exceed 150 characters")
        String label,

        @NotNull(message = "Node type is required")
        MenuNodeType nodeType,

        @Size(max = 200, message = "Route must not exceed 200 characters")
        String route,

        @Size(max = 50, message = "Icon must not exceed 50 characters")
        String icon,

        Long parentId,

        int sortOrder,

        boolean enabled
) {}
