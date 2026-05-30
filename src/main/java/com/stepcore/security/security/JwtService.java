package com.stepcore.security.security;

import com.stepcore.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_TENANT_SLUG = "tenant_slug";
    public static final String CLAIM_TENANT_PLAN = "tenant_plan";
    public static final String CLAIM_ROLES = "roles";

    private final JwtProperties jwtProperties;

    public String generateToken(final UserDetails userDetails) {
        return generateToken(userDetails, Map.of());
    }

    public String generateToken(final UserDetails userDetails, final Map<String, Object> extraClaims) {
        final long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.expirationMs()))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractTenantId(final String token) {
        final String raw = extractClaim(token, claims -> claims.get(CLAIM_TENANT_ID, String.class));
        return raw == null ? null : Long.valueOf(raw);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(final String token) {
        final Object raw = extractClaim(token, claims -> claims.get(CLAIM_ROLES));
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(raw));
    }

    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtService] - TOKEN_VALIDATION: invalid token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(final String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
