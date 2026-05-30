package com.stepcore.security.security;

import com.stepcore.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-not-for-production-use-at-all";
    private static final long EXPIRATION_MS = 3_600_000L;
    private static final long EXPIRED_MS = 1L;

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        final JwtProperties props = new JwtProperties(SECRET, EXPIRATION_MS);
        jwtService = new JwtService(props);
        userDetails = User.withUsername("test@example.com")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldGenerateNonEmptyToken() {
        final String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    void shouldExtractEmailFromToken() {
        final String token = jwtService.generateToken(userDetails);
        final String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnTrueWhenTokenIsValid() {
        final String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenBelongsToDifferentUser() {
        final String token = jwtService.generateToken(userDetails);
        final UserDetails otherUser = User.withUsername("other@example.com")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenIsExpired() throws InterruptedException {
        final JwtProperties expiredProps = new JwtProperties(SECRET, EXPIRED_MS);
        final JwtService expiredService = new JwtService(expiredProps);
        final String token = expiredService.generateToken(userDetails);
        Thread.sleep(10);
        assertThat(expiredService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void shouldThrowWhenTokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.extractEmail("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldEmbedAndExtractTenantClaims() {
        final Long tenantId = 42L;
        final String token = jwtService.generateToken(userDetails, Map.of(
                JwtService.CLAIM_TENANT_ID, tenantId.toString(),
                JwtService.CLAIM_TENANT_SLUG, "acme",
                JwtService.CLAIM_TENANT_PLAN, "PREMIUM"));

        assertThat(jwtService.extractTenantId(token)).isEqualTo(tenantId);
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnNullTenantIdWhenClaimMissing() {
        final String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractTenantId(token)).isNull();
    }
}
