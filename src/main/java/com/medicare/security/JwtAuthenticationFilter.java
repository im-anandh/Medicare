package com.medicare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A FILTER runs on EVERY incoming HTTP request, before it ever
 * reaches a @Controller. OncePerRequestFilter guarantees this logic
 * runs exactly one time per request (some servlet setups can
 * otherwise trigger filters more than once for a single request).
 *
 * This filter's job, on every request:
 *   1. Look for an "Authorization: Bearer <token>" header.
 *   2. If present, extract the username from the token (JwtUtil).
 *   3. Load that user's details (CustomUserDetailsService) and verify
 *      the token is genuinely valid for them.
 *   4. If valid, tell Spring Security "this request is authenticated
 *      as this user" by populating the SecurityContextHolder - after
 *      this, the rest of Spring (our controllers, and the
 *      authorization rules we'll define in SecurityConfig) can treat
 *      this request as logged in.
 *   5. If no token, or an invalid one, we do nothing here - we just
 *      let the request continue unauthenticated. It's SecurityConfig
 *      that later decides whether an unauthenticated request is
 *      actually allowed to reach a given endpoint (e.g. /api/auth/**
 *      is fine without auth; /api/patients isn't).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // strip off "Bearer "

        try {
            String username = jwtUtil.extractUsername(token);

            // Only attempt authentication if we found a username AND
            // there isn't already an authenticated user set for this
            // request (avoids redundant work).
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // An invalid/expired/malformed token should NOT crash the
            // request - it should just result in "not authenticated,"
            // which SecurityConfig's rules will then correctly reject
            // with a 401 if the endpoint requires login.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
