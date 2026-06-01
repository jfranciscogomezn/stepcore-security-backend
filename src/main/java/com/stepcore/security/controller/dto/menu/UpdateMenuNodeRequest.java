package com.stepcore.security.controller.dto.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMenuNodeRequest(
        @NotBlank(message = "Label is required")
        @Size(max = 150, message = "Label must not exceed 150 characters")
        String label,

        @Size(max = 200, message = "Route must not exceed 200 characters")
        String route,

        @Size(max = 50, message = "Icon must not exceed 50 characters")
        String icon,

        int sortOrder,

        boolean enabled
) {}
