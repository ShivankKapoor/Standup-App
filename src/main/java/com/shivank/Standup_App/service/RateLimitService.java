package com.shivank.Standup_App.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;

@Service
public class RateLimitService {
    
    @Autowired
    private IpAddressService ipAddressService;
    
    private final ConcurrentHashMap<String, AttemptInfo> authAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptInfo> authenticatedRequests = new ConcurrentHashMap<>();
    
    private static class AttemptInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private long windowStart = Instant.now().getEpochSecond();
        
        synchronized boolean canAttempt(int maxAttempts, long windowSeconds) {
            long currentTime = Instant.now().getEpochSecond();
            
            // Reset window if expired
            if (currentTime - windowStart >= windowSeconds) {
                count.set(0);
                windowStart = currentTime;
            }
            
            return count.incrementAndGet() <= maxAttempts;
        }
        
        synchronized int getCurrentCount() {
            long currentTime = Instant.now().getEpochSecond();
            
            // Reset window if expired
            if (currentTime - windowStart >= 3600) { // 1 hour
                count.set(0);
                windowStart = currentTime;
                return 0;
            }
            
            return count.get();
        }
    }
    
    public boolean checkAuthenticationRateLimit(String ipAddress) {
        // 5 attempts per hour per IP
        AttemptInfo attempts = authAttempts.computeIfAbsent(ipAddress, k -> new AttemptInfo());
        return attempts.canAttempt(5, 3600);
    }
    
    /**
     * Check authentication rate limit using request to get real IP
     * This method uses the IpAddressService to resolve the real client IP
     * from X-Forwarded-For header (for Cloudflare tunnels) or direct IP
     */
    public boolean checkAuthenticationRateLimit(HttpServletRequest request) {
        String realIpAddress = ipAddressService.getClientIpAddress(request);
        return checkAuthenticationRateLimit(realIpAddress);
    }
    
    /**
     * Get the real client IP from request for rate limiting purposes
     */
    public String getClientIpForRateLimit(HttpServletRequest request) {
        return ipAddressService.getClientIpAddress(request);
    }
    
    public boolean checkAuthenticatedRateLimit(String username) {
        // 60 requests per hour per user
        AttemptInfo attempts = authenticatedRequests.computeIfAbsent(username, k -> new AttemptInfo());
        return attempts.canAttempt(60, 3600);
    }
    
    public int getAuthenticationAttempts(String ipAddress) {
        AttemptInfo attempts = authAttempts.get(ipAddress);
        return attempts != null ? attempts.getCurrentCount() : 0;
    }
    
    /**
     * Get authentication attempts using request to resolve real IP
     */
    public int getAuthenticationAttempts(HttpServletRequest request) {
        String realIpAddress = ipAddressService.getClientIpAddress(request);
        return getAuthenticationAttempts(realIpAddress);
    }
    
    public boolean isAccountLocked(String ipAddress) {
        return getAuthenticationAttempts(ipAddress) >= 5;
    }
    
    /**
     * Check if account is locked using request to resolve real IP
     */
    public boolean isAccountLocked(HttpServletRequest request) {
        String realIpAddress = ipAddressService.getClientIpAddress(request);
        return isAccountLocked(realIpAddress);
    }
}
