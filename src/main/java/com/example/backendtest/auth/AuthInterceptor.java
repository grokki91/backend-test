package com.example.backendtest.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.backendtest.common.ApiException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects protected routes without a valid Bearer token and publishes the caller's
 * id and role as request attributes for controllers to authorize against.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID = "userId";
    public static final String ROLE = "role";
    public static final String EMAIL = "email";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw ApiException.unauthorized("Missing or malformed Authorization header");
        }
        try {
            Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()).trim());
            request.setAttribute(USER_ID, ((Number) claims.get("uid")).longValue());
            request.setAttribute(ROLE, String.valueOf(claims.get(ROLE)));
            request.setAttribute(EMAIL, claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw ApiException.unauthorized("Token expired");
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw ApiException.unauthorized("Invalid token");
        }
        return true;
    }
}
