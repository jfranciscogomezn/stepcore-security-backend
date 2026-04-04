package com.gmm.devengos.controller;

import com.gmm.devengos.common.ApiResponse;
import com.gmm.devengos.controller.dto.role.CreateRoleRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionIdsRequest;
import com.gmm.devengos.controller.dto.role.MenuOptionResponse;
import com.gmm.devengos.controller.dto.role.RoleResponse;
import com.gmm.devengos.controller.dto.role.UpdateRoleRequest;
import com.gmm.devengos.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable final Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody final CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(roleService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable final Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role deleted successfully"));
    }

    @GetMapping("/{id}/menu-options")
    public ResponseEntity<ApiResponse<List<MenuOptionResponse>>> getMenuOptions(@PathVariable final Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getMenuOptions(id)));
    }

    @PutMapping("/{id}/menu-options")
    public ResponseEntity<ApiResponse<RoleResponse>> assignMenuOptions(
            @PathVariable final Long id,
            @Valid @RequestBody final MenuOptionIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.assignMenuOptions(id, request)));
    }
}
