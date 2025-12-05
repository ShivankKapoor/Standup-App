package com.shivank.Standup_App.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class DiscordService {

    private static final Logger logger = Logger.getLogger(DiscordService.class.getName());

    @Value("${discord.webhook.url:}")
    private String webhookUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendMessage(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warning("Discord webhook URL is not configured");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("content", message);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                logger.info("Discord message sent successfully");
            } else {
                logger.warning("Failed to send Discord message. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending Discord message", e);
        }
    }

    public void sendDiscordEmbed(Map<String, Object> payload) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warning("Discord webhook URL is not configured");
            return;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                logger.info("Discord embed sent successfully");
            } else {
                logger.warning("Failed to send Discord embed. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending Discord embed", e);
        }
    }
}
