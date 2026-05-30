package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.platform.CreateTenantRequest;
import com.stepcore.security.controller.dto.platform.ProvisionTenantResponse;
import com.stepcore.security.controller.dto.platform.TenantResponse;
import com.stepcore.security.controller.dto.platform.UpdateTenantRequest;
import com.stepcore.security.service.PlatformTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provider-plane tenant administration. Only PLATFORM_ADMIN operators (platform tenant) may
 * access these endpoints; they sit outside regular tenant scope.
 */
@RestController
@RequestMapping("/api/v1/platform/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(platformTenantService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> findById(@PathVariable final Long id) {
        return ResponseEntity.ok(ApiResponse.ok(platformTenantService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProvisionTenantResponse>> create(
            @Valid @RequestBody final CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(platformTenantService.create(request),
                        "Tenant provisioned. Share the temporary admin password securely."));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> update(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateTenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(platformTenantService.update(id, request)));
    }
}
