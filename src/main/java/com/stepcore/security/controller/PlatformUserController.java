package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.platform.PlatformUserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.service.PlatformUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provider-plane endpoint for managing tenant users.
 * Accessible exclusively to PLATFORM_ADMIN operators.
 * Uses native queries to operate cross-tenant (bypasses Hibernate tenantFilter).
 */
@RestController
@RequestMapping("/api/v1/platform/tenants/{tenantId}/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlatformUserResponse>>> listByTenant(
            @PathVariable final Long tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(platformUserService.listByTenant(tenantId)));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<PlatformUserResponse>> setStatus(
            @PathVariable final Long tenantId,
            @PathVariable final Long userId,
            @Valid @RequestBody final UserStatusRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final PlatformUserResponse response = platformUserService.setStatus(
                tenantId, userId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
