package com.stepcore.security.controller.dto.role;

import java.util.List;

public record RoleResponse(Long id, String name, String description, List<MenuNodeResponse> menuNodes) {}
