package net.pchinese.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final UserRoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   AuthSessionRepository sessionRepository, UserRoleRepository roleRepository,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.roleRepository = roleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            JwtService.JwtClaims claims = jwtService.validate(header.substring(7));
            var user = userRepository.findById(claims.userId()).orElseThrow();
            boolean sessionActive = sessionRepository.isActiveForUser(claims.sessionId(), claims.userId());
            if (!sessionActive || !user.isActiveVerified() || user.getAuthzVersion() != claims.authzVersion()) {
                throw new JwtService.JwtValidationException("Invalid access token.", false);
            }
            Set<String> roles = roleRepository.findActiveRoleCodesByUserId(claims.userId());
            UserPrincipal principal = new UserPrincipal(claims.userId(), claims.sessionId(), claims.authzVersion(), roles);
            var authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toSet());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
            chain.doFilter(request, response);
        } catch (JwtService.JwtValidationException exception) {
            writeUnauthorized(response, exception.expired() ? "ACCESS_TOKEN_EXPIRED" : "UNAUTHENTICATED",
                    exception.expired() ? "Access token has expired." : "Authentication is required.");
        } catch (Exception exception) {
            writeUnauthorized(response, "UNAUTHENTICATED", "Authentication is required.");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiEnvelope.failure(code, message));
    }
}
