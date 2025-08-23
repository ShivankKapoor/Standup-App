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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoggingService {
    
    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId CST_ZONE = ZoneId.of("America/Chicago");
    
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
        
        // Send security events to Discord with rich formatting
        if (event.contains("LOGIN_FAILED") || event.contains("ACCOUNT_LOCKED") || event.contains("BRUTE_FORCE")) {
            sendDiscordNotification(createSecurityAlertEmbed(event, details, true));
        } else if (event.contains("LOGIN_SUCCESS")) {
            sendDiscordNotification(createSecurityAlertEmbed(event, details, false));
        }
    }
    
    public void logRateLimitViolation(String ip, String type, String details) {
        String message = String.format("%s - %s - %s", ip, type, details);
        logToFile("logs/rate_limit.log", message);
        
        // Create simple rate limit notification
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "⚠️ Rate Limit Violation: " + message);
        payload.put("username", "Standup App Security");
        sendDiscordNotification(payload);
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
    
    private void sendDiscordNotification(Map<String, Object> payload) {
        if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
            System.out.println("⚠️ Discord webhook URL is not configured");
            return;
        }
        
        System.out.println("📤 Sending Discord notification...");
        
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discordWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("📬 Discord response status: " + response.statusCode());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send Discord notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Map<String, Object> createSecurityAlertEmbed(String event, String details, boolean isAlert) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> embed = new HashMap<>();
        Map<String, Object> author = new HashMap<>();
        
        // Parse details to extract IP and username
        String ip = extractFromDetails(details, "IP: ");
        String username = extractFromDetails(details, "user: ");
        ZonedDateTime cstTime = ZonedDateTime.now(CST_ZONE);
        String timestamp = cstTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " CST";
        
        // Set author
        author.put("name", "Standup App Security Alert");
        author.put("icon_url", "https://cdn-icons-png.flaticon.com/512/2317/2317988.png");
        embed.put("author", author);
        
        // Set color and title based on event type
        if (isAlert) {
            embed.put("color", 15158332); // Red color
            embed.put("title", "🚨 Security Alert");
            embed.put("description", "Security violation detected");
        } else {
            embed.put("color", 3066993); // Green color
            embed.put("title", "✅ Successful Login");
            embed.put("description", "User logged in successfully");
        }
        
        // Add fields
        java.util.List<Map<String, Object>> fields = new java.util.ArrayList<>();
        
        // Authentication status field
        Map<String, Object> authField = new HashMap<>();
        authField.put("name", isAlert ? "❌ Authentication" : "✅ Authentication");
        authField.put("value", isAlert ? "Failed login attempt" : "Successful authentication from IP " + ip);
        authField.put("inline", false);
        fields.add(authField);
        
        // IP Address field
        Map<String, Object> ipField = new HashMap<>();
        ipField.put("name", "🌐 IP Address");
        ipField.put("value", ip != null ? ip : "Unknown");
        ipField.put("inline", true);
        fields.add(ipField);
        
        // Username field
        Map<String, Object> userField = new HashMap<>();
        userField.put("name", "👤 Username");
        userField.put("value", username != null ? username : "Unknown");
        userField.put("inline", true);
        fields.add(userField);
        
        // Time field
        Map<String, Object> timeField = new HashMap<>();
        timeField.put("name", "🕐 Time");
        timeField.put("value", timestamp);
        timeField.put("inline", true);
        fields.add(timeField);
        
        // Action field
        Map<String, Object> actionField = new HashMap<>();
        actionField.put("name", "🔒 Action");
        actionField.put("value", isAlert ? "Access denied" : "User authenticated");
        actionField.put("inline", false);
        fields.add(actionField);
        
        embed.put("fields", fields);
        
        // Set footer
        Map<String, Object> footer = new HashMap<>();
        footer.put("text", "Standup App Security Alert • " + cstTime.format(DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a")) + " CST");
        embed.put("footer", footer);
        
        // Set timestamp
        embed.put("timestamp", cstTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        
        payload.put("username", "Standup Bot");
        payload.put("embeds", java.util.Arrays.asList(embed));
        
        return payload;
    }
    
    private String extractFromDetails(String details, String prefix) {
        int startIndex = details.indexOf(prefix);
        if (startIndex == -1) return null;
        
        startIndex += prefix.length();
        int endIndex = details.indexOf(" - ", startIndex);
        if (endIndex == -1) {
            endIndex = details.indexOf(" ", startIndex);
            if (endIndex == -1) endIndex = details.length();
        }
        
        return details.substring(startIndex, endIndex).trim();
    }
}
