package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MenuController {

    private final RoleService roleService;

    @GetMapping("/catalogue")
    public ResponseEntity<ApiResponse<List<MenuTreeNode>>> getCatalogue() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getMenuCatalogue()));
    }
}
