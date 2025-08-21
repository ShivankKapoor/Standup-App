package com.shivank.Standup_App.util;

import com.shivank.Standup_App.service.StandupDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Component  // Uncomment to run this test on startup
public class FileFormatTester implements CommandLineRunner {
    
    @Autowired
    private StandupDataService standupDataService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Testing Standups.txt File Parsing ===");
        
        Map<String, String> standups = standupDataService.loadAllStandups();
        
        System.out.println("Loaded " + standups.size() + " standup entries:");
        
        // Show first few entries
        standups.entrySet().stream()
                .limit(5)
                .forEach(entry -> {
                    System.out.println("\nDate: " + entry.getKey());
                    System.out.println("Content: " + entry.getValue().substring(0, Math.min(100, entry.getValue().length())) + "...");
                });
        
        System.out.println("\n=== File parsing test complete ===");
    }
}
