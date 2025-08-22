# Testing the Redirect Fix

## The Problem You Identified

When a user visits `/` without a session cookie, the `AuthenticationInterceptor` was calling:
```java
response.sendRedirect("/login");
```

This caused HTTPS issues because:

1. **Protocol Mismatch**: The redirect didn't preserve the HTTPS protocol from Cloudflare
2. **Domain Issues**: The redirect might point to `localhost:5555` instead of your domain
3. **Proxy Headers**: The redirect didn't consider proxy headers from Cloudflare Tunnel

## The Fix Applied

### 1. **Proxy-Aware Redirects**
- Added `buildProxyAwareRedirectUrl()` method that checks Cloudflare headers
- Looks for `X-Forwarded-Proto`, `X-Forwarded-Host`, and `CF-Visitor` headers
- Builds correct HTTPS URLs when behind Cloudflare Tunnel

### 2. **Improved Cookie Handling**
- Explicitly set `cookie.setSecure(false)` since container runs HTTP
- Cloudflare handles HTTPS termination
- Added consistent cookie settings for login and logout

### 3. **Enhanced Proxy Configuration**
- Added `server.tomcat.use-relative-redirects=true`
- Configured internal proxy IP ranges for better security
- Added `server.tomcat.redirect-context-root=false`

## Testing Steps

1. **Build and run the container:**
   ```bash
   ./run.sh
   ```

2. **Test redirect behavior:**
   ```bash
   # Test that / redirects to /login properly
   curl -v http://localhost:5555/
   
   # Check headers in response
   curl -I http://localhost:5555/
   ```

3. **Test with Cloudflare Tunnel:**
   - Visit your domain directly (should redirect to login)
   - Login should work without HTTPS errors
   - Session should persist across requests

## Expected Behavior

- ✅ Visiting `/` without session → clean redirect to `/login`
- ✅ Login sets cookie correctly
- ✅ Session persists across requests
- ✅ No more HTTPS/SSL errors
- ✅ Works seamlessly with Cloudflare Tunnel

This fix addresses the exact issue you identified - the redirect mechanism that was causing HTTPS problems in your Cloudflare Tunnel setup!
