package com.shivank.Standup_App.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.shivank.Standup_App.service.ScheduledTaskService;
import com.shivank.Standup_App.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/uptime")
public class UptimeController {

    @Value("${app.uptime.max.millis:604800000}")
    private long maxUptimeMillis;

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Autowired
    private RateLimitService rateLimitService;

    /**
     * GET endpoint to check the current application uptime
     * Returns uptime in milliseconds and formatted string
     * Rate limited to 20 requests per hour per IP
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkUptime(HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.checkUptimeEndpointRateLimit(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Rate limit exceeded. Maximum 20 requests per hour.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            long uptimeMillis = runtime.getUptime();
            String formattedUptime = ScheduledTaskService.formatUptime(uptimeMillis);
            boolean isOverSevenDays = uptimeMillis >= maxUptimeMillis;
            String formattedThreshold = ScheduledTaskService.formatUptime(maxUptimeMillis);

            Map<String, Object> response = new HashMap<>();
            response.put("uptimeMillis", uptimeMillis);
            response.put("uptime", formattedUptime);
            response.put("isOverSevenDays", isOverSevenDays);
            response.put("thresholdMillis", maxUptimeMillis);
            response.put("threshold", formattedThreshold);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to check uptime: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * POST endpoint to manually trigger the cron job immediately
     * Runs the uptime check and sends Discord notification if over 7 days
     * Rate limited to 20 requests per hour per IP
     */
    @PostMapping("/trigger-check")
    public ResponseEntity<Map<String, Object>> triggerUptimeCheck(HttpServletRequest request) {
        // Check rate limit
        if (!rateLimitService.checkUptimeEndpointRateLimit(request)) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Rate limit exceeded. Maximum 20 requests per hour.");
            return ResponseEntity.status(429).body(error);
        }

        try {
            scheduledTaskService.checkUptimeAndNotify();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Uptime check triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to trigger uptime check: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
