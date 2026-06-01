package com.tourwise.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final List<String> PROTECTED_PREFIXES = List.of(
            "/user/me", "/user/profile", "/user/avatar", "/user/logout",
            "/user/spot-actions",
            "/recommend/rating", "/food/review", "/log/create", "/log/my",
            "/circle/create", "/upload", "/admin", "/route/records", "/itinerary/plans", "/itinerary/shared-plans",
            "/notifications", "/reports"
    );
    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Long userId = parseUserId(request.getHeader("Authorization"));
        if (userId != null) {
            AuthContext.setUserId(userId);
        }
        if (requiresLogin(path, request.getMethod()) && userId == null) {
            response.setStatus(401);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private Long parseUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.parseUserId(authorization.substring(7));
    }

    private boolean requiresLogin(String path, String method) {
        if ("DELETE".equalsIgnoreCase(method) && path.matches("/log/\\d+")) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("/log/\\d+/like")) {
            return true;
        }
        if (path.matches("/circle/\\d+/(join|leave|logs|like|comments)") && !"GET".equalsIgnoreCase(method)) {
            return true;
        }
        return PROTECTED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
