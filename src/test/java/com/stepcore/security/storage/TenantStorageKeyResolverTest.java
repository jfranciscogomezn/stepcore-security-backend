package com.stepcore.security.storage;

import com.stepcore.security.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantStorageKeyResolverTest {

    private final TenantStorageKeyResolver resolver = new TenantStorageKeyResolver();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldPrefixKeyWithCurrentTenant() {
        assertThat(resolver.resolveKey("osi", "123", "report.pdf"))
                .isEqualTo("7/osi/123/report.pdf");
    }

    @Test
    void shouldExposeCurrentTenantPrefix() {
        assertThat(resolver.currentTenantPrefix()).isEqualTo("7/");
    }

    @Test
    void shouldStripDirectoriesFromFileNameToPreventTraversal() {
        assertThat(resolver.resolveKey("field-log", "55", "../../etc/passwd"))
                .isEqualTo("7/field-log/55/passwd");
    }

    @Test
    void shouldFallBackToLegacyTenantWhenContextUnset() {
        TenantContext.clear();
        assertThat(resolver.currentTenantPrefix())
                .isEqualTo(TenantContext.LEGACY_TENANT_ID + "/");
    }

    @Test
    void shouldRejectKeyFromAnotherTenantOnAccessCheck() {
        assertThatThrownBy(() -> resolver.assertWithinCurrentTenant("8/osi/123/report.pdf"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAcceptKeyWithinCurrentTenantOnAccessCheck() {
        resolver.assertWithinCurrentTenant("7/osi/123/report.pdf");
    }

    @Test
    void shouldRejectBlankOrNullKeyOnAccessCheck() {
        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> resolver.assertWithinCurrentTenant(null));
        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> resolver.assertWithinCurrentTenant("   "));
    }

    @Test
    void shouldRejectInvalidPathSegments() {
        assertThatThrownBy(() -> resolver.resolveKey("os/i", "123", "f.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.resolveKey("osi", "..", "f.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> resolver.resolveKey("osi", "123", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
