package com.gmm.devengos.controller.dto.role;

import java.util.List;

public record RoleResponse(Long id, String name, String description, List<MenuOptionResponse> menuOptions) {}
