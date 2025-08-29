package com.shivank.Standup_App.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CalendarService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId CST_ZONE = ZoneId.of("America/Chicago");
    
    public static class CalendarDay {
        private LocalDate date;
        private boolean isCurrentMonth;
        private boolean isToday;
        private boolean hasEntry;
        private boolean isPTO;
        private boolean isPlanning;
        private boolean isSupport;

        public CalendarDay(LocalDate date, boolean isCurrentMonth, boolean isToday, boolean hasEntry, boolean isPTO, boolean isPlanning, boolean isSupport) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.hasEntry = hasEntry;
            this.isPTO = isPTO;
            this.isPlanning = isPlanning;
            this.isSupport = isSupport;
        }
        
        // Getters
        public LocalDate getDate() { return date; }
        public boolean isCurrentMonth() { return isCurrentMonth; }
        public boolean isToday() { return isToday; }
        public boolean isHasEntry() { return hasEntry; }
    public boolean isPTO() { return isPTO; }
    public boolean isPlanning() { return isPlanning; }
    public boolean isSupport() { return isSupport; }
        public String getDateString() { return date.format(DATE_FORMATTER); }
        public int getDayNumber() { return date.getDayOfMonth(); }
        
        public String getCssClasses() {
            StringBuilder classes = new StringBuilder("day-cell");
            if (!isCurrentMonth) classes.append(" other-month");
            if (isToday) classes.append(" today");
            if (hasEntry) classes.append(" has-entry");
            if (isPTO) classes.append(" pto-day");
            if (isPlanning) classes.append(" planning-day");
            if (isSupport) classes.append(" support-day");
            return classes.toString();
        }
    }
    
    public List<CalendarDay> generateCalendarDays(LocalDate displayDate, Map<String, String> standups) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate today = LocalDate.now(CST_ZONE);
        
        // Get first day of the month
        LocalDate firstDayOfMonth = displayDate.withDayOfMonth(1);
        
        // Get the day of week for the first day (0 = Sunday, 1 = Monday, etc.)
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;
        
        // Calculate the starting date (might be from previous month)
        LocalDate startDate = firstDayOfMonth.minusDays(firstDayOfWeek);
        
        // Generate 42 days (6 weeks)
        for (int i = 0; i < 42; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            boolean isCurrentMonth = currentDate.getMonth() == displayDate.getMonth() && 
                                   currentDate.getYear() == displayDate.getYear();
            boolean isToday = currentDate.equals(today);
            
            String dateKey = currentDate.format(DATE_FORMATTER);
            boolean hasEntry = standups.containsKey(dateKey);
            boolean isPTO = false;
            boolean isPlanning = false;
            boolean isSupport = false;

            // Check for special day types
            if (hasEntry) {
                String content = standups.get(dateKey);
                if (content != null) {
                    isPTO = content.startsWith("$(PTO)");
                    isPlanning = content.startsWith("$(Planning)");
                    isSupport = content.startsWith("$(Support)");
                }
            }

            days.add(new CalendarDay(currentDate, isCurrentMonth, isToday, hasEntry, isPTO, isPlanning, isSupport));
        }
        
        return days;
    }
}
