package com.stepcore.security.controller;

import com.stepcore.security.controller.dto.auth.LoginRequest;
import com.stepcore.security.controller.dto.platform.CreateTenantRequest;
import com.stepcore.security.domain.model.TenantPlan;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantIsolationControllerIT extends RlsIntegrationTest {

    private static final String PLATFORM_SLUG = "platform";
    private static final String PLATFORM_EMAIL = "platform@stepcore.com";
    private static final String PLATFORM_PASSWORD = "Platform@2026!";

    private static final String LEGACY_SLUG = "legacy";
    private static final String LEGACY_EMAIL = "admin@stepcore.com";
    private static final String LEGACY_PASSWORD = "Admin@2026!";

    @Test
    void shouldHideOtherTenantUserFromFindById() throws Exception {
        final ProvisionedTenant tenantB = provisionTenant();
        final String legacyToken = loginToken(LEGACY_SLUG, LEGACY_EMAIL, LEGACY_PASSWORD);

        mockMvc.perform(get("/api/v1/users/" + tenantB.adminUserId())
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListOnlyCurrentTenantUsers() throws Exception {
        final ProvisionedTenant tenantB = provisionTenant();
        final String legacyToken = loginToken(LEGACY_SLUG, LEGACY_EMAIL, LEGACY_PASSWORD);
        final String tenantBToken = loginToken(tenantB.slug(), tenantB.adminEmail(), tenantB.temporaryPassword());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].email").value(not(hasItem(tenantB.adminEmail()))));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].email").value(hasItem(tenantB.adminEmail())))
                .andExpect(jsonPath("$.data[*].email").value(not(hasItem(LEGACY_EMAIL))));
    }

    @Test
    void shouldHideOtherTenantRoleFromFindById() throws Exception {
        final ProvisionedTenant tenantB = provisionTenant();
        final String legacyToken = loginToken(LEGACY_SLUG, LEGACY_EMAIL, LEGACY_PASSWORD);

        mockMvc.perform(get("/api/v1/roles/" + tenantB.adminRoleId())
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectTenantAdminFromPlatformEndpoints() throws Exception {
        final String legacyToken = loginToken(LEGACY_SLUG, LEGACY_EMAIL, LEGACY_PASSWORD);

        mockMvc.perform(get("/api/v1/platform/tenants")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldProvisionTenantAsPlatformAdmin() throws Exception {
        final String platformToken = loginToken(PLATFORM_SLUG, PLATFORM_EMAIL, PLATFORM_PASSWORD);
        final String slug = "iso-" + UUID.randomUUID().toString().substring(0, 8);
        final CreateTenantRequest request = new CreateTenantRequest(
                "Isolation Test Tenant",
                slug,
                TenantPlan.STANDARD,
                null,
                slug + "@tenant.test",
                "Iso",
                "Admin");

        mockMvc.perform(post("/api/v1/platform/tenants")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tenant.slug").value(slug))
                .andExpect(jsonPath("$.data.adminEmail").value(slug + "@tenant.test"))
                .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty());
    }

    private ProvisionedTenant provisionTenant() throws Exception {
        final String platformToken = loginToken(PLATFORM_SLUG, PLATFORM_EMAIL, PLATFORM_PASSWORD);
        final String slug = "iso-" + UUID.randomUUID().toString().substring(0, 8);
        final String adminEmail = slug + "@tenant.test";
        final CreateTenantRequest request = new CreateTenantRequest(
                "Isolation Test Tenant",
                slug,
                TenantPlan.STANDARD,
                null,
                adminEmail,
                "Iso",
                "Admin");

        final String body = mockMvc.perform(post("/api/v1/platform/tenants")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String temporaryPassword = objectMapper.readTree(body).at("/data/temporaryPassword").asText();
        final String tenantBToken = loginToken(slug, adminEmail, temporaryPassword);

        final String usersBody = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final long adminUserId = objectMapper.readTree(usersBody).at("/data/0/id").asLong();

        final String rolesBody = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final long adminRoleId = objectMapper.readTree(rolesBody).at("/data/0/id").asLong();

        return new ProvisionedTenant(slug, adminEmail, temporaryPassword, adminUserId, adminRoleId);
    }

    private String loginToken(final String tenantSlug, final String email, final String password) throws Exception {
        final String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(tenantSlug, email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/token").asText();
    }

    private record ProvisionedTenant(
            String slug,
            String adminEmail,
            String temporaryPassword,
            long adminUserId,
            long adminRoleId
    ) {
    }
}
