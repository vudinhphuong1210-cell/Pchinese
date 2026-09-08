package net.pchinese.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.pchinese.common.api.ApiEnvelope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

@Component
public class RefreshRequestSecurityFilter extends OncePerRequestFilter {
    private static final String BROWSER_SESSION_HEADER = "X-Browser-Session-Id";
    private static final String LEGACY_CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_COOKIE_PREFIX = "XSRF-TOKEN-";
    private final PchineseSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public RefreshRequestSecurityFilter(PchineseSecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equals(request.getMethod()) || !("/api/v1/auth/refresh".equals(path) || "/api/v1/auth/logout".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String csrfCookie = selectedCsrfCookie(request);
        String csrfHeader = request.getHeader("X-CSRF-Token");
        if (csrfCookie == null || csrfHeader == null || !constantTimeEquals(csrfCookie, csrfHeader) || !trustedOrigin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiEnvelope.failure("AUTHORIZATION_DENIED", "Request origin validation failed."));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean trustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            String referer = request.getHeader("Referer");
            if (referer == null || referer.isBlank()) return false;
            try { origin = new URI(referer).getScheme() + "://" + new URI(referer).getAuthority(); }
            catch (Exception ignored) { return false; }
        }
        return properties.getAllowedOrigins().contains(origin);
    }
    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private String selectedCsrfCookie(HttpServletRequest request) {
        String requestedSessionId = request.getHeader(BROWSER_SESSION_HEADER);
        if (requestedSessionId != null && !requestedSessionId.isBlank()) {
            try {
                return cookie(request, CSRF_COOKIE_PREFIX + java.util.UUID.fromString(requestedSessionId));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        java.util.List<Cookie> namedCookies = new java.util.ArrayList<>();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().startsWith(CSRF_COOKIE_PREFIX)) namedCookies.add(cookie);
            }
        }
        if (namedCookies.size() == 1) return namedCookies.getFirst().getValue();
        if (namedCookies.size() > 1) return null;
        return cookie(request, LEGACY_CSRF_COOKIE);
    }
    private boolean constantTimeEquals(String first, String second) {
        return java.security.MessageDigest.isEqual(first.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                second.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
