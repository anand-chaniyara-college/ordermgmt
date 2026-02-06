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

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get the Authorization Header
        String authHeader = request.getHeader("Authorization");

        // 2. Check if it exists and starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // 3. Validate Token
            if (jwtUtil.validateToken(token)) {

                // 4. Extract User Info
                String email = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                // 5. Create Authentication Object
                // We add the role to the authorities list (Spring Security needs this to check
                // .hasAuthority("ADMIN"))
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(authority) // List of roles
                );

                // 6. Set the Security Context (Logs the user in for this request)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 7. Continue the Filter Chain (let the request proceed to the Controller)
        filterChain.doFilter(request, response);
    }
}
