package com.stepcore.security.storage;

import com.stepcore.security.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Builds tenant-partitioned object-storage keys of the form
 * {@code {tenant_id}/{domain}/{entity-id}/{file}} and guards that client-supplied keys
 * (downloads/deletes) belong to the current tenant.
 *
 * <p>The tenant prefix is always derived from {@link TenantContext} — never from client input —
 * so attachments are physically partitioned within the shared bucket and a crafted key cannot
 * reach another tenant's objects.</p>
 */
@Component
public class TenantStorageKeyResolver {

    private static final String SEPARATOR = "/";

    /** @return the current tenant's key prefix, e.g. {@code "2/"}. */
    public String currentTenantPrefix() {
        return TenantContext.getTenantIdOrDefault() + SEPARATOR;
    }

    /**
     * Builds a fully-qualified object key for the current tenant.
     *
     * @param domain   logical area (e.g. {@code "osi"}, {@code "field-log"}); a single path segment
     * @param entityId owning entity id; a single path segment
     * @param fileName original file name; any directory part is stripped to its base name
     * @return {@code {tenant_id}/{domain}/{entity-id}/{file}}
     */
    public String resolveKey(final String domain, final String entityId, final String fileName) {
        return currentTenantPrefix()
                + segment(domain) + SEPARATOR
                + segment(entityId) + SEPARATOR
                + baseFileName(fileName);
    }

    /**
     * Ensures a client-supplied key belongs to the current tenant before download/delete.
     *
     * @throws AccessDeniedException when the key is null, blank, or outside the tenant prefix
     */
    public void assertWithinCurrentTenant(final String key) {
        if (key == null || key.isBlank() || !key.startsWith(currentTenantPrefix())) {
            throw new AccessDeniedException("Storage key does not belong to the current tenant");
        }
    }

    private static String segment(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Storage path segment must not be blank");
        }
        final String trimmed = value.trim();
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.equals(".") || trimmed.equals("..")) {
            throw new IllegalArgumentException("Invalid storage path segment: " + value);
        }
        return trimmed;
    }

    private static String baseFileName(final String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name must not be blank");
        }
        final Path name = Paths.get(fileName.trim().replace('\\', '/')).getFileName();
        if (name == null) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        final String base = name.toString();
        if (base.isBlank() || base.equals(".") || base.equals("..")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return base;
    }
}
