package com.stepcore.security.controller.dto.platform;

import com.stepcore.security.domain.model.TenantPlan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to provision a new tenant. The provider supplies the company details, the plan,
 * and the initial tenant administrator; provisioning seeds the tenant ADMIN role, its menu
 * access and the admin account. When {@code maxUsers} is omitted the plan default is used.
 */
public record CreateTenantRequest(
        @NotBlank(message = "Tenant name is required")
        @Size(max = 150, message = "Tenant name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Tenant slug is required")
        @Size(max = 100, message = "Tenant slug must not exceed 100 characters")
        @Pattern(regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
                message = "Slug must be lowercase alphanumeric with optional hyphens")
        String slug,

        @NotNull(message = "Plan is required")
        TenantPlan plan,

        @Positive(message = "maxUsers must be a positive number")
        Integer maxUsers,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Admin email must be a valid email address")
        String adminEmail,

        @NotBlank(message = "Admin first name is required")
        @Size(max = 100, message = "Admin first name must not exceed 100 characters")
        String adminFirstName,

        @NotBlank(message = "Admin last name is required")
        @Size(max = 100, message = "Admin last name must not exceed 100 characters")
        String adminLastName
) {}
