package com.example.demo.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthenticationFilter(
            AuthService authService,
            UserRepository userRepository) {

        this.authService = authService;
        this.userRepository = userRepository;
    }

    // Skip JWT validation for public endpoints.
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        String path = request.getServletPath();
        String method = request.getMethod();

        // =====================================================
        // CORS PREFLIGHT
        // =====================================================

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // =====================================================
        // PUBLIC AUTHENTICATION ENDPOINTS
        // =====================================================

        if ("/api/auth/login".equals(path)
                || "/api/users/register".equals(path)
                || "/api/setup/initial-admin".equals(path)) {

            return true;
        }

        // =====================================================
        // RAZORPAY WEBHOOK
        // =====================================================

        // Razorpay webhook uses webhook signature authentication
        // instead of customer JWT authentication.
        if ("POST".equalsIgnoreCase(method)
                && "/api/webhooks/razorpay".equals(path)) {

            return true;
        }

        // =====================================================
        // PUBLIC PRODUCT ENDPOINTS
        // =====================================================

        if (HttpMethod.GET.matches(method)
                && ("/api/products".equals(path)
                || isPublicProductEndpoint(path))) {

            return true;
        }

        // =====================================================
        // PUBLIC CUSTOMER CATEGORY ENDPOINTS
        // =====================================================

        // IMPORTANT:
        //
        // /api/categories
        // /api/categories/1
        //
        // are public.
        //
        // But:
        //
        // /api/categories/admin/all
        //
        // is an ADMIN endpoint and must pass through
        // JWT authentication.

        if (HttpMethod.GET.matches(method)
                && ("/api/categories".equals(path)
                || isPublicCategoryEndpoint(path))) {

            return true;
        }

        return false;
    }

    // =========================================================
    // CHECK PUBLIC PRODUCT ENDPOINT
    // =========================================================

    private boolean isPublicProductEndpoint(String path) {

        /*
         * Public product endpoint:
         *
         * GET /api/products/{productId}
         *
         * Example:
         *
         * /api/products/1
         * /api/products/2
         * /api/products/25
         *
         * But:
         *
         * /api/products/admin/all
         *
         * is NOT public.
         * It must pass through the authentication filter.
         */

        if (!path.startsWith("/api/products/")) {
            return false;
        }

        String remainingPath =
                path.substring("/api/products/".length());

        // Admin product endpoints must NOT be skipped.
        if (remainingPath.startsWith("admin/")) {
            return false;
        }

        // Only numeric product IDs are public.
        return remainingPath.matches("\\d+");
    }

    // =========================================================
    // CHECK PUBLIC CATEGORY ENDPOINT
    // =========================================================

    private boolean isPublicCategoryEndpoint(String path) {

        /*
         * Public category endpoint:
         *
         * GET /api/categories/{categoryId}
         *
         * Example:
         *
         * /api/categories/1
         * /api/categories/2
         *
         * But:
         *
         * /api/categories/admin/all
         *
         * is NOT public.
         * It must pass through the authentication filter.
         */

        if (!path.startsWith("/api/categories/")) {
            return false;
        }

        String remainingPath =
                path.substring("/api/categories/".length());

        // Admin category endpoint
        if (remainingPath.startsWith("admin/")) {
            return false;
        }

        // Only numeric category IDs are public.
        return remainingPath.matches("\\d+");
    }

    // =========================================================
    // JWT AUTHENTICATION
    // =========================================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = getAuthTokenFromCookies(request);

        // No JWT cookie
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // =================================================
            // VALIDATE JWT
            // =================================================

            if (!authService.validateToken(token)) {

                SecurityContextHolder.clearContext();

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired authentication token");

                return;
            }

            // =================================================
            // EXTRACT USERNAME
            // =================================================

            String username =
                    authService.extractUsername(token);

            // =================================================
            // LOAD USER FROM DATABASE
            // =================================================

            User user = userRepository
                    .findByUsername(username)
                    .orElse(null);

            if (user == null) {

                SecurityContextHolder.clearContext();

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "User not found");

                return;
            }

            // =================================================
            // CREATE SPRING SECURITY AUTHORITY
            // =================================================

            /*
             * CUSTOMER -> ROLE_CUSTOMER
             * ADMIN    -> ROLE_ADMIN
             */

            var authority = new SimpleGrantedAuthority(
                    "ROLE_" + user.getRole().name());

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(authority));

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request));

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication failed");

            return;
        }

        filterChain.doFilter(request, response);
    }

    // =========================================================
    // GET JWT FROM COOKIE
    // =========================================================

    private String getAuthTokenFromCookies(
            HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie ->
                        "authToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}