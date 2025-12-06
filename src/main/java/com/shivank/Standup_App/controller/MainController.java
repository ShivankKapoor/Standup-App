package com.shivank.Standup_App.controller;

import com.shivank.Standup_App.service.CalendarService;
import com.shivank.Standup_App.service.IpAddressService;
import com.shivank.Standup_App.service.LoggingService;
import com.shivank.Standup_App.service.RateLimitService;
import com.shivank.Standup_App.service.SessionService;
import com.shivank.Standup_App.service.StandupDataService;
import com.shivank.Standup_App.service.TwoFactorService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private StandupDataService standupDataService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private CalendarService calendarService;
    
    @Autowired
    private IpAddressService ipAddressService;
    
    @Autowired
    private TwoFactorService twoFactorService;
    
    @Value("${auth.username}")
    private String authUsername;
    
    @Value("${auth.password}")
    private String authPassword;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId CST_ZONE = ZoneId.of("America/Chicago");
    
    // Debug endpoint to check auth values
    @GetMapping("/debug-auth")
    @ResponseBody
    public String debugAuth() {
        return "Username: '" + authUsername + "', Password: '" + authPassword + "'";
    }
    
    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        String currentUser = (String) request.getAttribute("currentUser");
        
        // Get current month or requested month
        String monthParam = request.getParameter("month");
        LocalDate displayDate = LocalDate.now(CST_ZONE);
        
        if (monthParam != null) {
            try {
                displayDate = LocalDate.parse(monthParam + "-01");
            } catch (DateTimeParseException e) {
                // Invalid month format, use current month
            }
        }
        
        Map<String, String> standups = standupDataService.loadAllStandups();
        List<CalendarService.CalendarDay> calendarDays = calendarService.generateCalendarDays(displayDate, standups);
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("displayDate", displayDate);
        model.addAttribute("calendarDays", calendarDays);
        model.addAttribute("today", LocalDate.now(CST_ZONE));
        
        return "index";
    }
    
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }
    
    @PostMapping("/login")
    public String loginPost(@RequestParam String username, 
                           @RequestParam String password,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        
        String ip = ipAddressService.getClientIpAddress(request);
        
        // Check authentication rate limiting
        if (!rateLimitService.checkAuthenticationRateLimit(request)) {
            loggingService.logRateLimitViolation(ip, "AUTH_ATTEMPT", 
                    "Exceeded 5 login attempts per hour");
            model.addAttribute("error", "Too many login attempts. Please try again later.");
            return "login";
        }
        
        // Check if account is locked
        if (rateLimitService.isAccountLocked(request)) {
            loggingService.logSecurityEvent("ACCOUNT_LOCKED", 
                    "IP: " + ip + " - Account locked due to failed attempts");
            model.addAttribute("error", "Account temporarily locked due to multiple failed attempts.");
            return "login";
        }
        
        // Validate credentials (username case-insensitive, password case-sensitive)
        if (!authUsername.equalsIgnoreCase(username) || !authPassword.equals(password)) {
            loggingService.logSecurityEvent("LOGIN_FAILED", 
                    "IP: " + ip + " - Invalid credentials for User: " + username);
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
        
        // Credentials are valid, redirect to 2FA verification
        // Store the username in the session for 2FA step
        // Force session creation and ensure it's persisted before redirect
        var session = request.getSession(true);
        session.setAttribute("2fa_pending_user", username);
        
        // Ensure session is created and ID is set before redirect
        // This forces Tomcat to commit the session cookie
        String sessionId = session.getId();
        
        loggingService.logAppEvent("User " + username + " from IP " + ip + " passed credentials (session: " + sessionId + "), awaiting 2FA");
        
        return "redirect:/2fa";
    }
    
    @GetMapping("/2fa")
    public String verify2fa(@RequestParam(value = "error", required = false) String error, 
                           HttpServletRequest request,
                           Model model) {
        // Check if user has passed credentials
        // Use getSession(false) to retrieve existing session without creating a new one
        var session = request.getSession(false);
        
        if (session == null) {
            loggingService.logSecurityEvent("2FA_NO_SESSION", 
                    "IP: " + ipAddressService.getClientIpAddress(request) + " - Attempted to access 2FA without session");
            return "redirect:/login";
        }
        
        String pendingUser = (String) session.getAttribute("2fa_pending_user");
        if (pendingUser == null) {
            loggingService.logSecurityEvent("2FA_NO_PENDING_USER", 
                    "IP: " + ipAddressService.getClientIpAddress(request) + " - Session exists but no pending user");
            return "redirect:/login";
        }
        
        if (error != null) {
            model.addAttribute("error", "Invalid 2FA code. Please try again.");
        }
        
        return "2fa";
    }
    
    @PostMapping("/2fa")
    public String verify2faPost(@RequestParam String code,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model) {
        
        var session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        
        String pendingUser = (String) session.getAttribute("2fa_pending_user");
        if (pendingUser == null) {
            return "redirect:/login";
        }
        
        String ip = ipAddressService.getClientIpAddress(request);
        
        // Validate 2FA code
        if (!twoFactorService.validateCode(code)) {
            loggingService.logSecurityEvent("2FA_FAILED", 
                    "IP: " + ip + " - User: " + pendingUser + " failed 2FA verification");
            return "redirect:/2fa?error=true";
        }
        
        // 2FA passed, create session token
        String sessionToken = sessionService.createSessionToken(pendingUser);
        
        // Set cookie
        Cookie cookie = new Cookie("session_token", sessionToken);
        cookie.setMaxAge(8 * 3600); // 8 hours
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false);
        response.addCookie(cookie);
        
        // Clear the pending 2FA user from session
        request.getSession().removeAttribute("2fa_pending_user");
        
        loggingService.logSecurityEvent("LOGIN_SUCCESS", 
                "IP: " + ip + " - User: " + pendingUser + " logged in successfully (2FA verified)");
        
        return "redirect:/";
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String currentUser = (String) request.getAttribute("currentUser");
        String ip = ipAddressService.getClientIpAddress(request);
        
        // Clear session cookie with same settings as when it was set
        Cookie cookie = new Cookie("session_token", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false); // Match login cookie settings
        response.addCookie(cookie);
        
        if (currentUser != null) {
            loggingService.logSecurityEvent("LOGOUT", 
                    "IP: " + ip + " - User: " + currentUser + " logged out");
        }
        
        return "logout";
    }
    
    @GetMapping("/entry/{date}")
    public String viewEntry(@PathVariable String date, Model model, HttpServletRequest request) {
        try {
            LocalDate entryDate = LocalDate.parse(date, DATE_FORMATTER);
            String content = standupDataService.getStandup(entryDate);
            
            model.addAttribute("date", entryDate);
            model.addAttribute("content", content);
            model.addAttribute("hasContent", content != null && !content.trim().isEmpty());
            
            return "entry";
        } catch (DateTimeParseException e) {
            return "redirect:/";
        }
    }
    
    @GetMapping("/edit/{date}")
    public String editEntry(@PathVariable String date, Model model, HttpServletRequest request) {
        try {
            LocalDate entryDate = LocalDate.parse(date, DATE_FORMATTER);
            String content = standupDataService.getStandup(entryDate);
            
            String dayType = "";
            String actualContent = content != null ? content : "";
            
            // Check for special day types and extract them
            if (content != null) {
                if (content.startsWith("$(PTO)")) {
                    dayType = "PTO";
                    actualContent = content.substring(6).trim(); // Remove "$(PTO)" and trim
                } else if (content.startsWith("$(Planning)")) {
                    dayType = "Planning";
                    actualContent = content.substring(11).trim(); // Remove "$(Planning)" and trim
                }
            }
            
            model.addAttribute("date", entryDate);
            model.addAttribute("content", actualContent);
            model.addAttribute("dayType", dayType);
            
            return "edit";
        } catch (DateTimeParseException e) {
            return "redirect:/";
        }
    }
    
    @PostMapping("/save/{date}")
    public String saveEntry(@PathVariable String date, 
                           @RequestParam String content,
                           @RequestParam(required = false) String dayType,
                           HttpServletRequest request) {
        try {
            LocalDate entryDate = LocalDate.parse(date, DATE_FORMATTER);
            String currentUser = (String) request.getAttribute("currentUser");
            
            // Sanitize and limit input
            if (content != null && content.length() > 50000) {
                content = content.substring(0, 50000);
            }
            
            // Handle special day types
            if (dayType != null && !dayType.isEmpty()) {
                if ("PTO".equals(dayType)) {
                    content = "$(PTO)";
                } else if ("Planning".equals(dayType)) {
                    content = "$(Planning)";
                } else if ("Support".equals(dayType)) {
                    content = "$(Support)";
                }
            }
            
            standupDataService.saveStandup(entryDate, content);
            
            loggingService.logAppEvent("Entry saved for date: " + date + " by user: " + currentUser);
            
            return "redirect:/entry/" + date;
        } catch (DateTimeParseException e) {
            return "redirect:/";
        }
    }
    
    @PostMapping("/delete/{date}")
    public String deleteEntry(@PathVariable String date, HttpServletRequest request) {
        try {
            LocalDate entryDate = LocalDate.parse(date, DATE_FORMATTER);
            String currentUser = (String) request.getAttribute("currentUser");
            
            standupDataService.deleteStandup(entryDate);
            
            loggingService.logAppEvent("Entry deleted for date: " + date + " by user: " + currentUser);
            
            return "redirect:/";
        } catch (DateTimeParseException e) {
            return "redirect:/";
        }
    }
}
