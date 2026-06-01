package com.stepcore.security.controller;

import com.stepcore.security.common.ApiResponse;
import com.stepcore.security.controller.dto.menu.CreateMenuNodeRequest;
import com.stepcore.security.controller.dto.menu.MenuNodeAdminResponse;
import com.stepcore.security.controller.dto.menu.UpdateMenuNodeRequest;
import com.stepcore.security.controller.dto.role.MenuTreeNode;
import com.stepcore.security.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/catalogue")
    @PreAuthorize("hasAnyRole('ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuTreeNode>>> getCatalogue(final Authentication authentication) {
        final boolean includeDisabled = isPlatformAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.ok(menuService.getCatalogue(includeDisabled)));
    }

    @PostMapping("/nodes")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<MenuNodeAdminResponse>> create(
            @Valid @RequestBody final CreateMenuNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(menuService.create(request)));
    }

    @PutMapping("/nodes/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<MenuNodeAdminResponse>> update(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateMenuNodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.update(id, request)));
    }

    @DeleteMapping("/nodes/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable final Long id) {
        menuService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Menu node deleted successfully"));
    }

    private static boolean isPlatformAdmin(final Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals);
    }
}
