package com.shivank.Standup_App.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CalendarService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public static class CalendarDay {
        private LocalDate date;
        private boolean isCurrentMonth;
        private boolean isToday;
        private boolean hasEntry;
        
        public CalendarDay(LocalDate date, boolean isCurrentMonth, boolean isToday, boolean hasEntry) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.hasEntry = hasEntry;
        }
        
        // Getters
        public LocalDate getDate() { return date; }
        public boolean isCurrentMonth() { return isCurrentMonth; }
        public boolean isToday() { return isToday; }
        public boolean isHasEntry() { return hasEntry; }
        public String getDateString() { return date.format(DATE_FORMATTER); }
        public int getDayNumber() { return date.getDayOfMonth(); }
        
        public String getCssClasses() {
            StringBuilder classes = new StringBuilder("day-cell");
            if (!isCurrentMonth) classes.append(" other-month");
            if (isToday) classes.append(" today");
            if (hasEntry) classes.append(" has-entry");
            return classes.toString();
        }
    }
    
    public List<CalendarDay> generateCalendarDays(LocalDate displayDate, Map<String, String> standups) {
        List<CalendarDay> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
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
            boolean hasEntry = standups.containsKey(currentDate.format(DATE_FORMATTER));
            
            days.add(new CalendarDay(currentDate, isCurrentMonth, isToday, hasEntry));
        }
        
        return days;
    }
}
