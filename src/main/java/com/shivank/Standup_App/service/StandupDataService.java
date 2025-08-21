package com.shivank.Standup_App.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class StandupDataService {
    
    @Value("${standups.file}")
    private String standupsFilePath;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public Map<String, String> loadAllStandups() {
        Map<String, String> standups = new HashMap<>();
        
        try {
            Path path = Paths.get(standupsFilePath);
            
            // Create parent directories if they don't exist
            Files.createDirectories(path.getParent());
            
            if (!Files.exists(path)) {
                Files.createFile(path);
                return standups;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                String currentDate = null;
                StringBuilder currentContent = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Check if line is a date in format like "9_10_24"
                    if (line.matches("\\d{1,2}_\\d{1,2}_\\d{2}")) {
                        // Save previous entry if exists
                        if (currentDate != null && currentContent.length() > 0) {
                            String normalizedDate = convertToStandardDate(currentDate);
                            if (normalizedDate != null) {
                                standups.put(normalizedDate, currentContent.toString().trim());
                            }
                        }
                        
                        // Start new entry
                        currentDate = line;
                        currentContent = new StringBuilder();
                    } else if (line.equals("-------------------------------------------------------------------")) {
                        // End of current entry - save it
                        if (currentDate != null && currentContent.length() > 0) {
                            String normalizedDate = convertToStandardDate(currentDate);
                            if (normalizedDate != null) {
                                standups.put(normalizedDate, currentContent.toString().trim());
                            }
                        }
                        currentDate = null;
                        currentContent = new StringBuilder();
                    } else if (!line.isEmpty() && currentDate != null) {
                        // Add content line
                        if (currentContent.length() > 0) {
                            currentContent.append("\n");
                        }
                        currentContent.append(line);
                    }
                }
                
                // Save the last entry if it exists
                if (currentDate != null && currentContent.length() > 0) {
                    String normalizedDate = convertToStandardDate(currentDate);
                    if (normalizedDate != null) {
                        standups.put(normalizedDate, currentContent.toString().trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load standups", e);
        }
        
        return standups;
    }
    
    private String convertToStandardDate(String dateStr) {
        try {
            // Convert from "9_10_24" to "2024-09-10"
            String[] parts = dateStr.split("_");
            if (parts.length == 3) {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = 2000 + Integer.parseInt(parts[2]); // Assuming 21st century
                
                return String.format("%04d-%02d-%02d", year, month, day);
            }
        } catch (NumberFormatException e) {
            // Invalid date format
        }
        return null;
    }
    
    public String getStandup(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        return loadAllStandups().get(dateStr);
    }
    
    public void saveStandup(LocalDate date, String content) {
        String dateStr = date.format(DATE_FORMATTER);
        Map<String, String> standups = loadAllStandups();
        
        if (content == null || content.trim().isEmpty()) {
            standups.remove(dateStr);
        } else {
            standups.put(dateStr, content.trim());
        }
        
        saveAllStandups(standups);
    }
    
    public void deleteStandup(LocalDate date) {
        saveStandup(date, null);
    }
    
    private void saveAllStandups(Map<String, String> standups) {
        try {
            Path path = Paths.get(standupsFilePath);
            Files.createDirectories(path.getParent());
            
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                // Sort entries by date
                standups.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        try {
                            String standardDate = entry.getKey(); // yyyy-MM-dd format
                            String originalFormat = convertToOriginalFormat(standardDate);
                            String content = entry.getValue();
                            
                            if (originalFormat != null) {
                                writer.write(originalFormat);
                                writer.newLine();
                                writer.write(content);
                                writer.newLine();
                                writer.write("-------------------------------------------------------------------");
                                writer.newLine();
                                writer.newLine();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write entry", e);
                        }
                    });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save standups", e);
        }
    }
    
    private String convertToOriginalFormat(String standardDate) {
        try {
            // Convert from "2024-09-10" to "9_10_24"
            LocalDate date = LocalDate.parse(standardDate, DATE_FORMATTER);
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();
            int year = date.getYear() % 100; // Get last 2 digits of year
            
            return String.format("%d_%d_%02d", month, day, year);
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean hasStandup(LocalDate date) {
        String content = getStandup(date);
        return content != null && !content.trim().isEmpty();
    }
}
