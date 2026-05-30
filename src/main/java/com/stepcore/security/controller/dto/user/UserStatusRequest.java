package com.stepcore.security.controller.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull(message = "enabled flag is required")
        Boolean enabled
) {}
