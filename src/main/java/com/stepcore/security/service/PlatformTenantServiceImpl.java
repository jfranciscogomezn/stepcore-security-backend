package com.stepcore.security.service;

import com.stepcore.security.controller.dto.platform.CreateTenantRequest;
import com.stepcore.security.controller.dto.platform.ProvisionTenantResponse;
import com.stepcore.security.controller.dto.platform.TenantResponse;
import com.stepcore.security.controller.dto.platform.UpdateTenantRequest;
import com.stepcore.security.domain.model.Tenant;
import com.stepcore.security.domain.model.TenantStatus;
import com.stepcore.security.exception.TenantNotFoundException;
import com.stepcore.security.exception.TenantSlugAlreadyExistsException;
import com.stepcore.security.repository.TenantRepository;
import com.stepcore.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PlatformTenantServiceImpl implements PlatformTenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantProvisioningService provisioningService;

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> findAll() {
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse findById(final Long id) {
        return toResponse(getTenantOrThrow(id));
    }

    @Override
    public ProvisionTenantResponse create(final CreateTenantRequest request) {
        final String slug = request.slug().toLowerCase();
        if (tenantRepository.existsBySlug(slug)) {
            throw new TenantSlugAlreadyExistsException(slug);
        }

        final int maxUsers = request.maxUsers() != null
                ? request.maxUsers()
                : request.plan().defaultMaxUsers();

        // Persist the tenant as PROVISIONING first, seed its baseline, then mark ACTIVE.
        final Tenant tenant = tenantRepository.save(Tenant.builder()
                .withName(request.name())
                .withSlug(slug)
                .withPlan(request.plan())
                .withMaxUsers(maxUsers)
                .withStatus(TenantStatus.PROVISIONING)
                .withPlatform(false)
                .build());

        final String temporaryPassword = provisioningService.provisionInitialAdmin(
                tenant, request.adminEmail(), request.adminFirstName(), request.adminLastName());

        tenant.activate();
        final Tenant active = tenantRepository.save(tenant);

        log.info("[PlatformTenantServiceImpl] - CREATE_TENANT: slug={} plan={} maxUsers={}",
                active.getSlug(), active.getPlan(), active.getMaxUsers());

        return new ProvisionTenantResponse(toResponse(active), request.adminEmail(), temporaryPassword);
    }

    @Override
    public TenantResponse update(final Long id, final UpdateTenantRequest request) {
        final Tenant tenant = getTenantOrThrow(id);

        if (request.plan() != null || request.maxUsers() != null) {
            final var newPlan = request.plan() != null ? request.plan() : tenant.getPlan();
            final int newMaxUsers = request.maxUsers() != null
                    ? request.maxUsers()
                    : tenant.getMaxUsers();
            tenant.updatePlan(newPlan, newMaxUsers);
        }

        if (request.status() == TenantStatus.SUSPENDED) {
            tenant.suspend();
        } else if (request.status() == TenantStatus.ACTIVE) {
            tenant.activate();
        }

        final Tenant saved = tenantRepository.save(tenant);
        log.info("[PlatformTenantServiceImpl] - UPDATE_TENANT: slug={} plan={} maxUsers={} status={}",
                saved.getSlug(), saved.getPlan(), saved.getMaxUsers(), saved.getStatus());
        return toResponse(saved);
    }

    private Tenant getTenantOrThrow(final Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    private TenantResponse toResponse(final Tenant tenant) {
        return TenantResponse.from(tenant, userRepository.countByTenantId(tenant.getId()));
    }
}
