package com.shivank.Standup_App.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@Service
public class ScheduledTaskService {

    private static final Logger logger = Logger.getLogger(ScheduledTaskService.class.getName());

    @Value("${app.uptime.max.millis:604800000}")
    private long maxUptimeMillis;

    @Autowired
    private DiscordService discordService;
    
    @Autowired
    private SessionService sessionService;

    /**
     * Runs every day at 12:00 AM CST to clean up expired session tokens
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "America/Chicago")
    public void cleanupExpiredSessions() {
        try {
            sessionService.cleanupExpiredSessions();
            logger.info("Cleaned up expired session tokens");
        } catch (Exception e) {
            logger.severe("Error cleaning up expired sessions: " + e.getMessage());
        }
    }

    /**
     * Runs every day at 7 PM CST (1 AM UTC the next day)
     * Cron format: second minute hour day month weekday
     * 0 0 1 * * * = 1 AM UTC (7 PM CST when CST is UTC-6)
     * Note: For CDT (UTC-5), this would be 6 PM CDT. Adjust accordingly.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "America/Chicago")
    public void checkUptimeAndNotify() {
        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            long uptimeMillis = runtime.getUptime();

            logger.info("Current application uptime: " + ScheduledTaskService.formatUptime(uptimeMillis));

            if (uptimeMillis >= maxUptimeMillis) {
                // Create Discord embed similar to security alerts
                Map<String, Object> embed = new HashMap<>();
                Map<String, Object> author = new HashMap<>();
                
                ZonedDateTime cstTime = ZonedDateTime.now(ZoneId.of("America/Chicago"));
                
                // Set author
                author.put("name", "Standup App Security Alert");
                author.put("icon_url", "https://cdn-icons-png.flaticon.com/512/2317/2317988.png");
                embed.put("author", author);
                
                // Set color (yellow for warning) and title
                embed.put("color", 16776960); // Yellow color
                embed.put("title", "⚠️ Extended Uptime Alert");
                embed.put("description", "Application uptime has exceeded 7 days and requires restart");
                
                // Add fields
                List<Map<String, Object>> fields = new ArrayList<>();
                
                // Uptime field
                Map<String, Object> uptimeField = new HashMap<>();
                uptimeField.put("name", "⏱️ Current Uptime");
                uptimeField.put("value", ScheduledTaskService.formatUptime(uptimeMillis));
                uptimeField.put("inline", false);
                fields.add(uptimeField);
                
                // Status field
                Map<String, Object> statusField = new HashMap<>();
                statusField.put("name", "🔴 Status");
                statusField.put("value", "REQUIRES RESTART");
                statusField.put("inline", true);
                fields.add(statusField);
                
                // Severity field
                Map<String, Object> severityField = new HashMap<>();
                severityField.put("name", "📊 Severity");
                severityField.put("value", "Medium");
                severityField.put("inline", true);
                fields.add(severityField);
                
                // Action field
                Map<String, Object> actionField = new HashMap<>();
                actionField.put("name", "🔒 Required Action");
                actionField.put("value", "Restart the application to refresh the security key");
                actionField.put("inline", false);
                fields.add(actionField);
                
                embed.put("fields", fields);
                
                // Set footer
                Map<String, Object> footer = new HashMap<>();
                footer.put("text", "Standup App Security Alert • " + cstTime.format(DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a")) + " CST");
                embed.put("footer", footer);
                
                // Set timestamp
                embed.put("timestamp", cstTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                
                // Create payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", "Standup Bot");
                payload.put("embeds", Arrays.asList(embed));
                
                discordService.sendDiscordEmbed(payload);
                logger.info("Discord notification sent for high uptime");
            }
        } catch (Exception e) {
            logger.severe("Error in scheduled uptime check: " + e.getMessage());
        }
    }

    /**
     * Formats uptime from milliseconds to a readable string
     */
    public static String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours = hours % 24;
        minutes = minutes % 60;
        seconds = seconds % 60;

        return String.format("%d days, %d hours, %d minutes, %d seconds (%d milliseconds)", 
                            days, hours, minutes, seconds, milliseconds);
    }
}
