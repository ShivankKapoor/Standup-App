package com.shivank.Standup_App.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    
    @Value("${auth.secret.key}")
    private String secretKey;
    
    @Value("${session.timeout.seconds:900}")
    private long sessionTimeoutSeconds;
    
    private final long appStartTime;
    private final ConcurrentHashMap<String, Long> activeSessions;
    
    public SessionService() {
        this.appStartTime = Instant.now().getEpochSecond();
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    public String createSessionToken(String username) {
        try {
            long currentTime = Instant.now().getEpochSecond();
            String timestamp = String.valueOf(currentTime);
            String appInstanceId = String.valueOf(appStartTime);
            String payload = username + "|" + timestamp + "|" + appInstanceId;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(hash);
            
            String token = payload + "|" + signature;
            
            // Store token in active sessions
            activeSessions.put(token, currentTime);
            
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create session token", e);
        }
    }
    
    public String validateSessionToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // Check if token exists in active sessions
        if (!activeSessions.containsKey(token)) {
            return null;
        }
        
        try {
            String[] parts = token.split("\\|");
            if (parts.length != 4) {
                return null;
            }
            
            String username = parts[0];
            String timestamp = parts[1];
            String appInstanceId = parts[2];
            String signature = parts[3];
            
            // Verify HMAC signature first
            String payload = username + "|" + timestamp + "|" + appInstanceId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(hash);
            
            if (!signature.equals(expectedSignature)) {
                return null;
            }
            
            // Check session timeout expiration
            long tokenTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            if (currentTime - tokenTime > sessionTimeoutSeconds) {
                activeSessions.remove(token); // Clean up expired token
                return null;
            }
            
            // Validate app instance (sessions expire on app restart)
            if (!appInstanceId.equals(String.valueOf(appStartTime))) {
                return null;
            }
            
            return username;
        } catch (Exception e) {
            // Invalid token format or crypto error
        }
        
        return null;
    }
    
    public void invalidateSession(String token) {
        if (token != null && !token.isEmpty()) {
            activeSessions.remove(token);
        }
    }
    
    public int cleanupExpiredSessions() {
        long currentTime = Instant.now().getEpochSecond();
        int beforeSize = activeSessions.size();
        activeSessions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > sessionTimeoutSeconds
        );
        int afterSize = activeSessions.size();
        return beforeSize - afterSize;
    }
    
    public long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }
    
    public long getRemainingSessionSeconds(String token) {
        if (token == null || token.isEmpty()) {
            return sessionTimeoutSeconds; // Return full timeout if no token
        }
        
        try {
            String[] parts = token.split("\\|");
            if (parts.length != 4) {
                return sessionTimeoutSeconds;
            }
            
            String timestamp = parts[1];
            long tokenTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            long elapsed = currentTime - tokenTime;
            long remaining = sessionTimeoutSeconds - elapsed;
            
            return Math.max(0, remaining);
        } catch (Exception e) {
            return sessionTimeoutSeconds;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
