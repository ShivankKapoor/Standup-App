package com.shivank.Standup_App.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorService {
    
    @Value("${auth.2fa.secret}")
    private String twoFactorSecret;
    
    private final CodeVerifier verifier;
    
    public TwoFactorService() {
        TimeProvider timeProvider = new SystemTimeProvider();
        this.verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);
    }
    
    /**
     * Validates a 6-digit TOTP code from Google Authenticator
     * @param code The 6-digit code from the user
     * @return true if valid, false otherwise
     */
    public boolean validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Remove any whitespace
            code = code.replaceAll("\\s+", "");
            
            // Validate it's 6 digits
            if (!code.matches("^\\d{6}$")) {
                return false;
            }
            
            // Verify the code with time window
            return verifier.isValidCode(twoFactorSecret, code);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the 2FA secret key to display to user for Google Authenticator setup
     * @return The base32-encoded secret key
     */
    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }
}
