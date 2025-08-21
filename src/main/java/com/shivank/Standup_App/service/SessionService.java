package com.shivank.Standup_App.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class SessionService {
    
    @Value("${auth.secret.key}")
    private String secretKey;
    
    private final long appStartTime;
    
    public SessionService() {
        this.appStartTime = Instant.now().getEpochSecond();
    }
    
    public String createSessionToken(String username) {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String appInstanceId = String.valueOf(appStartTime);
            String payload = username + "|" + timestamp + "|" + appInstanceId;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(hash);
            
            return payload + "|" + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create session token", e);
        }
    }
    
    public String validateSessionToken(String token) {
        if (token == null || token.isEmpty()) {
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
            
            // Validate app instance (sessions expire on app restart)
            if (!appInstanceId.equals(String.valueOf(appStartTime))) {
                return null;
            }
            
            // Check 8-hour expiration
            long tokenTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            if (currentTime - tokenTime > 8 * 3600) {
                return null;
            }
            
            // Verify HMAC signature
            String payload = username + "|" + timestamp + "|" + appInstanceId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(hash);
            
            if (signature.equals(expectedSignature)) {
                return username;
            }
        } catch (Exception e) {
            // Invalid token format or crypto error
        }
        
        return null;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
