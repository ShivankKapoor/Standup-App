package com.shivank.Standup_App.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;

@Service
public class RateLimitService {
    
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
    
    public boolean checkAuthenticatedRateLimit(String username) {
        // 60 requests per hour per user
        AttemptInfo attempts = authenticatedRequests.computeIfAbsent(username, k -> new AttemptInfo());
        return attempts.canAttempt(60, 3600);
    }
    
    public int getAuthenticationAttempts(String ipAddress) {
        AttemptInfo attempts = authAttempts.get(ipAddress);
        return attempts != null ? attempts.getCurrentCount() : 0;
    }
    
    public boolean isAccountLocked(String ipAddress) {
        return getAuthenticationAttempts(ipAddress) >= 5;
    }
}
