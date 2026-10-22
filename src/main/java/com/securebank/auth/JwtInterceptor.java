package com.securebank.auth;

import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every protected controller method.
 * 1. Reads "Authorization: Bearer <token>"
 * 2. Validates the token
 * 3. Puts the CurrentUser on the request
 * 4. Blocks non-admins from /api/admin/**
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Browsers send OPTIONS before cross-origin calls; let them through.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("Missing or invalid Authorization header");
        }

        Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()))
                .orElseThrow(() -> ApiException.unauthorized("Token is invalid or expired"));

        CurrentUser user = new CurrentUser(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                Role.valueOf(claims.get("role", String.class)));

        if (request.getRequestURI().startsWith("/api/admin") && !user.isAdmin()) {
            throw ApiException.forbidden("Admin access required");
        }

        request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, user);
        return true;
    }
}
