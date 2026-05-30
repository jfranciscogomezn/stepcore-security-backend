package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.user.CreateUserRequest;
import com.stepcore.security.controller.dto.user.UpdateUserRequest;
import com.stepcore.security.controller.dto.user.UserResponse;
import com.stepcore.security.controller.dto.user.UserStatusRequest;
import com.stepcore.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> findById(
            @PathVariable final Long id,
            @AuthenticationPrincipal final UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody final CreateUserRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        final UserResponse response = userService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateUserRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request, userDetails.getUsername())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable final Long id,
            @AuthenticationPrincipal final UserDetails userDetails) {
        userService.delete(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> setStatus(
            @PathVariable final Long id,
            @Valid @RequestBody final UserStatusRequest request,
            @AuthenticationPrincipal final UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.setStatus(id, request, userDetails.getUsername())));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable final Long id,
            @AuthenticationPrincipal final UserDetails userDetails) {
        userService.resetPassword(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully. User must change password on next login."));
    }
}
