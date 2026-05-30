package com.stepcore.security.service;

import com.stepcore.security.controller.dto.platform.CreateTenantRequest;
import com.stepcore.security.controller.dto.platform.ProvisionTenantResponse;
import com.stepcore.security.controller.dto.platform.TenantResponse;
import com.stepcore.security.controller.dto.platform.UpdateTenantRequest;

import java.util.List;

/**
 * Provider-plane operations to manage SaaS tenants. Restricted to PLATFORM_ADMIN.
 */
public interface PlatformTenantService {

    List<TenantResponse> findAll();

    TenantResponse findById(Long id);

    ProvisionTenantResponse create(CreateTenantRequest request);

    TenantResponse update(Long id, UpdateTenantRequest request);
}
