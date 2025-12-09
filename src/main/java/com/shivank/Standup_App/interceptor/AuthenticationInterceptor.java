package com.shivank.Standup_App.interceptor;

import com.shivank.Standup_App.service.IpAddressService;
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
    
    @Autowired
    private IpAddressService ipAddressService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String ip = ipAddressService.getClientIpAddress(request);
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
            
            // Use relative redirect to preserve proxy headers and domain
            String loginUrl = buildProxyAwareRedirectUrl(request, "/login");
            response.sendRedirect(loginUrl);
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
               uri.equals("/2fa") ||
               uri.equals("/health") ||
               uri.equals("/admin/cleanup-sessions") ||
               uri.startsWith("/api/uptime/") ||
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
    
    /**
     * Build a proxy-aware redirect URL that works with Cloudflare Tunnel
     * This preserves the correct protocol and domain when behind a proxy
     */
    private String buildProxyAwareRedirectUrl(HttpServletRequest request, String path) {
        // Check if we're behind a proxy (Cloudflare Tunnel)
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String cfVisitorProto = request.getHeader("CF-Visitor");
        
        // If we have Cloudflare headers, use them
        if (forwardedProto != null && forwardedHost != null) {
            return forwardedProto + "://" + forwardedHost + path;
        }
        
        // If we have CF-Visitor header (Cloudflare specific)
        if (cfVisitorProto != null && cfVisitorProto.contains("https")) {
            String host = request.getHeader("Host");
            if (host != null) {
                return "https://" + host + path;
            }
        }
        
        // Fallback to relative redirect (safest for proxy setups)
        return path;
    }
}
