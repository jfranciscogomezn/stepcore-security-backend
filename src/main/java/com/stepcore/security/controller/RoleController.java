package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.role.CreateRoleRequest;
import com.stepcore.security.controller.dto.role.MenuNodeIdsRequest;
import com.stepcore.security.controller.dto.role.MenuNodeResponse;
import com.stepcore.security.controller.dto.role.RoleResponse;
import com.stepcore.security.controller.dto.role.UpdateRoleRequest;
import com.stepcore.security.service.RoleService;
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

    @GetMapping("/{id}/menu-nodes")
    public ResponseEntity<ApiResponse<List<MenuNodeResponse>>> getAssignedMenuNodes(@PathVariable final Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getAssignedMenuNodes(id)));
    }

    @PutMapping("/{id}/menu-nodes")
    public ResponseEntity<ApiResponse<RoleResponse>> assignMenuNodes(
            @PathVariable final Long id,
            @Valid @RequestBody final MenuNodeIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.assignMenuNodes(id, request)));
    }
}
