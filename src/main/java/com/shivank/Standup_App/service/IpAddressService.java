package com.shivank.Standup_App.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IpAddressService {
    
    /**
     * Extracts the real client IP address from the request.
     * Prioritizes X-Forwarded-For header (for Cloudflare tunnels and proxies)
     * Falls back to direct connection IP if header is not present.
     * 
     * @param request The HTTP request
     * @return The client's real IP address
     */
    public String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header first (Cloudflare tunnel, reverse proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // The first IP is the original client
            String clientIp = xForwardedFor.split(",")[0].trim();
            if (isValidIpAddress(clientIp)) {
                return clientIp;
            }
        }
        
        // Check other common proxy headers as fallback
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && isValidIpAddress(xRealIp)) {
            return xRealIp;
        }
        
        String xClientIp = request.getHeader("X-Client-IP");
        if (StringUtils.hasText(xClientIp) && isValidIpAddress(xClientIp)) {
            return xClientIp;
        }
        
        // Fallback to direct connection IP
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
    
    /**
     * Basic validation to ensure the IP address is not obviously invalid
     * 
     * @param ip The IP address to validate
     * @return true if the IP appears valid
     */
    private boolean isValidIpAddress(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        // Filter out common invalid values
        String trimmedIp = ip.trim().toLowerCase();
        return !trimmedIp.equals("unknown") && 
               !trimmedIp.equals("localhost") && 
               !trimmedIp.equals("127.0.0.1") &&
               !trimmedIp.equals("::1") &&
               !trimmedIp.startsWith("192.168.") && // Skip private IPs in X-Forwarded-For
               !trimmedIp.startsWith("10.") &&
               !trimmedIp.startsWith("172.") &&
               trimmedIp.length() >= 7; // Minimum valid IP length
    }
    
    /**
     * Gets a sanitized version of the IP for logging purposes
     * 
     * @param request The HTTP request
     * @return Sanitized IP address safe for logging
     */
    public String getLoggableIpAddress(HttpServletRequest request) {
        String ip = getClientIpAddress(request);
        
        // For IPv6, truncate to first 4 segments for privacy
        if (ip.contains(":") && ip.split(":").length > 4) {
            String[] segments = ip.split(":");
            return String.join(":", segments[0], segments[1], segments[2], segments[3]) + ":xxxx:xxxx:xxxx:xxxx";
        }
        
        return ip;
    }
    
    /**
     * Debug method to show all IP-related headers
     * Useful for troubleshooting proxy configurations
     * 
     * @param request The HTTP request
     * @return String with all IP-related headers for debugging
     */
    public String getDebugIpInfo(HttpServletRequest request) {
        StringBuilder debug = new StringBuilder();
        debug.append("IP Debug Info:\n");
        debug.append("X-Forwarded-For: ").append(request.getHeader("X-Forwarded-For")).append("\n");
        debug.append("X-Real-IP: ").append(request.getHeader("X-Real-IP")).append("\n");
        debug.append("X-Client-IP: ").append(request.getHeader("X-Client-IP")).append("\n");
        debug.append("Remote-Addr: ").append(request.getRemoteAddr()).append("\n");
        debug.append("Resolved IP: ").append(getClientIpAddress(request)).append("\n");
        return debug.toString();
    }
}
