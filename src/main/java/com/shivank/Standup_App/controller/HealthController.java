package com.shivank.Standup_App.controller;

import com.shivank.Standup_App.service.IpAddressService;
import com.shivank.Standup_App.service.LoggingService;
import com.shivank.Standup_App.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    private static final Instant START_TIME = Instant.now();
    private static final ZoneId CST_ZONE = ZoneId.of("America/Chicago");
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private IpAddressService ipAddressService;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        String ip = ipAddressService.getClientIpAddress(request);
        
        // Check health endpoint rate limiting (10 per hour)
        if (!rateLimitService.checkHealthEndpointRateLimit(request)) {
            loggingService.logRateLimitViolation(ip, "HEALTH_CHECK", 
                    "Exceeded 10 health check requests per hour");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "RATE_LIMITED");
            errorResponse.put("message", "Too many health check requests. Limit: 10 per hour");
            errorResponse.put("timestamp", LocalDateTime.now(CST_ZONE)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }
        
        Map<String, Object> healthInfo = new HashMap<>();
        
        // Basic status
        healthInfo.put("status", "UP");
        healthInfo.put("service", "Standup App");
        healthInfo.put("version", "1.0.0");
        
        // Uptime information
        Duration uptime = Duration.between(START_TIME, Instant.now());
        healthInfo.put("uptime", formatUptime(uptime));
        healthInfo.put("uptimeSeconds", uptime.getSeconds());
        
        // Timestamp information
        LocalDateTime now = LocalDateTime.now(CST_ZONE);
        healthInfo.put("timestamp", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        healthInfo.put("timezone", "America/Chicago");
        
        // Application startup time
        healthInfo.put("startTime", START_TIME.atZone(CST_ZONE)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // JVM information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("maxMemory", formatBytes(runtime.maxMemory()));
        jvmInfo.put("totalMemory", formatBytes(runtime.totalMemory()));
        jvmInfo.put("freeMemory", formatBytes(runtime.freeMemory()));
        jvmInfo.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        jvmInfo.put("availableProcessors", runtime.availableProcessors());
        
        // JVM uptime (alternative method)
        long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
        jvmInfo.put("jvmUptimeMs", jvmUptime);
        jvmInfo.put("jvmUptime", formatUptime(Duration.ofMillis(jvmUptime)));
        
        healthInfo.put("jvm", jvmInfo);
        
        // System information
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        
        healthInfo.put("system", systemInfo);
        
        return ResponseEntity.ok(healthInfo);
    }
    
    @GetMapping("/health/simple")
    public ResponseEntity<Map<String, Object>> simpleHealth(HttpServletRequest request) {
        String ip = ipAddressService.getClientIpAddress(request);
        
        // Check health endpoint rate limiting (10 per hour)
        if (!rateLimitService.checkHealthEndpointRateLimit(request)) {
            loggingService.logRateLimitViolation(ip, "HEALTH_CHECK_SIMPLE", 
                    "Exceeded 10 health check requests per hour");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "RATE_LIMITED");
            errorResponse.put("message", "Too many health check requests. Limit: 10 per hour");
            errorResponse.put("timestamp", LocalDateTime.now(CST_ZONE)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }
        
        Map<String, Object> simpleHealthInfo = new HashMap<>();
        
        // Basic status
        simpleHealthInfo.put("status", "UP");
        simpleHealthInfo.put("service", "Standup App");
        
        // Uptime information
        Duration uptime = Duration.between(START_TIME, Instant.now());
        simpleHealthInfo.put("uptime", formatUptime(uptime));
        simpleHealthInfo.put("uptimeSeconds", uptime.getSeconds());
        
        // Timestamp
        LocalDateTime now = LocalDateTime.now(CST_ZONE);
        simpleHealthInfo.put("timestamp", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return ResponseEntity.ok(simpleHealthInfo);
    }
    
    private String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;
        long seconds = uptime.getSeconds() % 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
