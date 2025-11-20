package com.shopping.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
  // config/JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();
    // ✅ Skip JWT for webhook
    if (path.startsWith("/jay/webhook/cashfree")) {
        log.info("Skipping JWT filter for webhook path: {}", path);
        filterChain.doFilter(request, response);
        return;
    }

    String header = request.getHeader("Authorization");
    log.info("=== JWT FILTER START ===");
    log.info("Request: {} {}", request.getMethod(), path);

    if (header == null || !header.startsWith("Bearer ")) {
        log.warn("No Bearer token");
        filterChain.doFilter(request, response);
        return;
    }

    String token = header.substring(7);
    log.info("JWT Token: {}", token);

    try {
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);
        log.info("Extracted Email: {} | Role: {}", email, role);

        if (email == null) {
            log.error("Email null from JWT");
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.info("Already authenticated");
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        log.info("UserDetails loaded: {}", userDetails != null ? userDetails.getUsername() : "NULL");

        if (userDetails == null) {
            log.error("UserDetails NULL");
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtUtil.validateToken(token, userDetails)) {
            log.info("Token VALID");

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // THIS LINE WAS MISSING!
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("Authentication SET in SecurityContext");
            log.info("Authorities: {}", userDetails.getAuthorities());
        } else {
            log.warn("Token INVALID");
        }
    } catch (Exception e) {
        log.error("JWT Error: {}", e.getMessage(), e);
    }

    log.info("=== JWT FILTER END ===");
    filterChain.doFilter(request, response);
}
}