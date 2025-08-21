package com.shivank.Standup_App.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoggingService {
    
    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void logAppEvent(String message) {
        logToFile("logs/app.log", message);
    }
    
    public void logAccessEvent(String ip, String method, String endpoint, String userAgent, String username) {
        String message = String.format("%s - %s %s - User: %s - UserAgent: %s", 
                ip, method, endpoint, username != null ? username : "anonymous", userAgent);
        logToFile("logs/access.log", message);
    }
    
    public void logSecurityEvent(String event, String details) {
        String message = String.format("%s - %s", event, details);
        logToFile("logs/security.log", message);
        
        // Send critical security events to Discord
        if (event.contains("LOGIN_FAILED") || event.contains("ACCOUNT_LOCKED") || event.contains("BRUTE_FORCE")) {
            sendDiscordNotification("🚨 Security Alert: " + message);
        }
    }
    
    public void logRateLimitViolation(String ip, String type, String details) {
        String message = String.format("%s - %s - %s", ip, type, details);
        logToFile("logs/rate_limit.log", message);
        sendDiscordNotification("⚠️ Rate Limit Violation: " + message);
    }
    
    private void logToFile(String filename, String message) {
        try {
            Path logPath = Paths.get(filename);
            Files.createDirectories(logPath.getParent());
            
            String timestampedMessage = LocalDateTime.now().format(formatter) + " - " + message + "\n";
            
            Files.write(logPath, timestampedMessage.getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to log file " + filename + ": " + e.getMessage());
        }
    }
    
    private void sendDiscordNotification(String message) {
        if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", message);
            payload.put("username", "Standup App Security");
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discordWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }
}
