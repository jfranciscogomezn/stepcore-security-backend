package com.stepcore.security.controller.dto.role;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MenuOptionIdsRequest(
        @NotNull(message = "menuOptionIds is required")
        List<Long> menuOptionIds
) {}
