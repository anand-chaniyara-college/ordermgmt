package com.example.ordermgmt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ordermgmt.service.TokenBlacklistService;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (tokenBlacklistService.isBlacklisted(token)) {
                    logger.warn("Authentication failed: Token is blacklisted. Request URI: {}", request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write("{\"error\": \"Authentication Failed\", \"message\": \"Token is blacklisted\"}");
                    return;
                }

                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    UUID orgId = jwtUtil.extractOrgId(token);
                    if (ROLE_SUPER_ADMIN.equals(role)) {
                        TenantContextHolder.setTenantId(TenantContextHolder.ROOT_TENANT_ID);
                    } else if (orgId != null) {
                        TenantContextHolder.setTenantId(orgId);
                    } else {
                        logger.warn("Authentication failed: Missing org_id claim for role {}. Request URI: {}", role,
                                request.getRequestURI());
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter()
                                .write("{\"error\": \"Authentication Failed\", \"message\": \"Missing org_id claim\"}");
                        return;
                    }

                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email, null, Collections.singletonList(authority));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Authentication successful for user: {}, role: {}", email, role);
                    }
                } else {
                    logger.warn("Authentication failed: Invalid JWT token. Request URI: {}", request.getRequestURI());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
