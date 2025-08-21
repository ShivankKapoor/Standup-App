package com.shivank.Standup_App.interceptor;

import com.shivank.Standup_App.service.LoggingService;
import com.shivank.Standup_App.service.RateLimitService;
import com.shivank.Standup_App.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private LoggingService loggingService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Skip authentication for certain endpoints
        if (isPublicEndpoint(requestURI)) {
            loggingService.logAccessEvent(ip, method, requestURI, userAgent, null);
            return true;
        }
        
        // Get session token from cookie
        String sessionToken = getSessionTokenFromCookies(request);
        String username = sessionService.validateSessionToken(sessionToken);
        
        if (username == null) {
            loggingService.logAccessEvent(ip, method, requestURI, userAgent, "UNAUTHENTICATED");
            response.sendRedirect("/login");
            return false;
        }
        
        // Check authenticated user rate limiting
        if (!rateLimitService.checkAuthenticatedRateLimit(username)) {
            loggingService.logRateLimitViolation(ip, "AUTHENTICATED_REQUEST", 
                    "User: " + username + " exceeded 60 requests/hour");
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            return false;
        }
        
        // Store username in request attribute for use in controllers
        request.setAttribute("currentUser", username);
        
        loggingService.logAccessEvent(ip, method, requestURI, userAgent, username);
        return true;
    }
    
    private boolean isPublicEndpoint(String uri) {
        return uri.equals("/login") || 
               uri.equals("/logout") || 
               uri.equals("/health") ||
               uri.startsWith("/css/") ||
               uri.startsWith("/js/") ||
               uri.startsWith("/images/") ||
               uri.equals("/favicon.ico");
    }
    
    private String getSessionTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("session_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
