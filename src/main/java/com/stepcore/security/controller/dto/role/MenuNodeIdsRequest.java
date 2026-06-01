package com.stepcore.security.controller.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MenuNodeIdsRequest(
        @NotNull @NotEmpty List<Long> menuNodeIds
) {}
