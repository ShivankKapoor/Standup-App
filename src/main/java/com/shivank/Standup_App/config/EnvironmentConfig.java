package com.shivank.Standup_App.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvironmentConfig {

    @PostConstruct
    public void loadEnvFile() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            
            // Set system properties from .env file
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
            
            System.out.println("✅ Loaded .env file with " + dotenv.entries().size() + " variables");
        } catch (Exception e) {
            System.out.println("⚠️ Could not load .env file: " + e.getMessage());
        }
    }
}
