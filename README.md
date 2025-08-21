# 📋 Standup App - Spring Boot Migration

A Spring Boot web application that exactly replicates the Flask standup app functionality with enhanced security and Cloudflare tunnel compatibility.

## Features

### Core Functionality
- **Interactive calendar view** with month navigation
- **Visual indicators**: Orange dots for entries, green dot for today
- **Click-to-edit**: Click any date to view/edit standup entries
- **File-based storage**: Maintains existing `Standups.txt` format
- **Auto-save**: Immediate persistence on submission
- **Data preservation**: Maintains all existing entries

### Authentication System
- **Custom session tokens** using HMAC with app instance validation
- **Session cookies** with `httponly=True`, `samesite='Lax'`, 8-hour expiration
- **Secure login page** with gradient styling
- **Session validation** on every protected route
- **Logout functionality** with confirmation and cookie clearing

### Security Features
- **Two-tier rate limiting**:
  - Authentication attempts: 5 per hour per IP
  - Authenticated requests: 60 per hour per user
- **Account lockout** after 5 failed attempts
- **Input sanitization** and 50KB limit
- **Brute force protection** with logging
- **CSRF protection** (token-based, 1-hour lifetime)

### Monitoring & Logging
- **Four separate log files**:
  - `logs/app.log` - Application events
  - `logs/access.log` - HTTP access logs
  - `logs/security.log` - Security events
  - `logs/rate_limit.log` - Rate limiting violations
- **Discord webhook notifications** for security events
- **Health check endpoint**: `/health`

## Quick Start

⚠️ **SECURITY FIRST**: Before running the application, ensure your credentials are properly configured and not committed to Git.

### Environment Setup

1. **Copy the environment template**:
   ```bash
   cp env.example .env
   ```

2. **Configure your credentials in `.env`**:
   ```bash
   # NEVER commit .env to Git - it contains your passwords!
   AUTH_USERNAME=your_username
   AUTH_PASSWORD=your_secure_password
   FLASK_SECRET_KEY=your_secret_key_here
   DISCORD_WEBHOOK_URL=your_discord_webhook_url
   ```

3. **Verify `.env` is ignored by Git**:
   ```bash
   git check-ignore .env  # Should return ".env"
   ```

### Option 1: Docker (Recommended)

```bash
# Clone and navigate to the project
git clone <repository-url>
cd standup-app

# Create your .env file with credentials
cp env.example .env
# Edit .env with your actual credentials

# Build and run with Docker Compose
docker-compose up --build

# Access the application
open http://localhost:5555
```

**Note**: The `.env` file is automatically copied into the Docker container during build, so your credentials will be available to the application.

### Option 2: Local Development

```bash
# Ensure Java 17+ and Maven are installed
java -version
mvn -version

# Set environment variables (copy from .env file)
export AUTH_USERNAME=your_username
export AUTH_PASSWORD="your_secure_password"
export FLASK_SECRET_KEY=your_secret_key_here
export STANDUPS_FILE=./data/Standups.txt

# Build and run the application
mvn clean install
mvn spring-boot:run

# Access the application
open http://localhost:5000
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AUTH_USERNAME` | *(none)* | Login username |
| `AUTH_PASSWORD` | *(none)* | Login password |
| `FLASK_SECRET_KEY` | *(none)* | HMAC signing key |
| `WTF_CSRF_TIME_LIMIT` | `3600` | CSRF token lifetime (seconds) |
| `STANDUPS_FILE` | `/app/data/Standups.txt` | Data file path |
| `DISCORD_WEBHOOK_URL` | *(none)* | Security notifications |

### Data Format

The `Standups.txt` file uses this exact format:
```
9_10_24 
Yesterday I worked on writing the Wiki for Synergy setup on mac.
Today looking for another US to work on. While I look i was thinking ill add some of the things that are 
different for mac on hawkexpress also to the Wiki. Also working on getting Jenkins access.
-------------------------------------------------------------------

9_11_24 
Yesterday learned about what SIMS does and how it works. I tried out the QA sims environment too. 
Also updated the icon for hawkexpress so its no longer the default angular icon. 

Today going to try and deploy SIMS on jboss and learn more about it so I can write some documentation on the WIKI over it.
-------------------------------------------------------------------
```

**Format Details:**
- **Date Line**: `M_D_YY` format (e.g., `9_10_24` for September 10, 2024)
- **Content**: Multi-line standup entry content
- **Separator**: `-------------------------------------------------------------------` to mark end of entry
- **Parsing**: Automatically converts dates to standard `YYYY-MM-DD` format internally
- **Preservation**: Maintains original format when saving new entries

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Main calendar view |
| `/login` | GET/POST | Login page and authentication |
| `/logout` | GET | Logout and clear session |
| `/entry/{date}` | GET | View standup entry |
| `/edit/{date}` | GET | Edit standup entry |
| `/save/{date}` | POST | Save standup entry |
| `/delete/{date}` | POST | Delete standup entry |
| `/health` | GET | Health check endpoint |

## Authentication Flow

1. **Unauthenticated users** → Redirect to `/login`
2. **Login page** → POST form with username/password
3. **Successful login** → Create HMAC session token, set cookie, redirect to `/`
4. **Protected routes** → Validate session token from cookie
5. **Logout** → Clear session cookie, redirect to logout confirmation

### Session Token Structure

Session tokens are HMAC-signed strings with 4 parts:
```
username|timestamp|app_instance_id|hmac_signature
```

- **Timestamp**: Enables 8-hour expiration
- **App instance ID**: Invalidates sessions on app restart
- **HMAC signature**: Prevents token tampering

## Security Features

### Rate Limiting
- **Authentication**: 5 attempts/hour per IP address
- **Authenticated requests**: 60 requests/hour per user
- **Account lockout**: 1 hour after 5 failed attempts

### Logging
All security events are logged with IP addresses, timestamps, and sent to Discord:
- Login attempts (success/failure)
- Rate limit violations
- Account lockouts
- Brute force detection

### Input Validation
- Content limited to 50KB
- HTML sanitization
- Date format validation
- CSRF token verification

## UI Components

### Login Page
- Gradient background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`
- White login box with form fields
- "📋 Standup App" header
- Error messages for failed attempts
- Footer: "Secure login • Session expires in 8 hours"

### Calendar View
- Month navigation with previous/next arrows
- Date grid with visual indicators
- Orange dots for dates with entries
- Green dot for today's date
- Click handlers for date navigation

### Entry Pages
- View page for reading entries
- Edit page with large textarea
- Delete functionality with confirmation
- Save/Cancel buttons with proper styling

### Logout System
- Red gradient button on every page
- JavaScript confirmation dialog
- Logout confirmation page with 3-second redirect

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/shivank/Standup_App/
│   │   ├── controller/MainController.java
│   │   ├── service/
│   │   │   ├── SessionService.java
│   │   │   ├── RateLimitService.java
│   │   │   ├── StandupDataService.java
│   │   │   ├── LoggingService.java
│   │   │   └── CalendarService.java
│   │   ├── interceptor/AuthenticationInterceptor.java
│   │   ├── config/WebConfig.java
│   │   └── StandupAppApplication.java
│   └── resources/
│       ├── templates/
│       │   ├── index.html
│       │   ├── login.html
│       │   ├── logout.html
│       │   ├── entry.html
│       │   └── edit.html
│       └── application.properties
├── test/
└── target/
```

### Building

```bash
# Clean and build
mvn clean install

# Run tests
mvn test

# Package for deployment
mvn package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

### Docker Build

```bash
# Build Docker image (includes Maven build and .env file)
docker build -t standup-app .

# Run container with volume mounts for data persistence
docker run -p 5555:5555 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  standup-app

# Or run in background
docker run -d -p 5555:5555 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  --name standup-app \
  standup-app
```

**Note**: The Docker image includes the `.env` file, so no environment variables need to be passed manually.

## Deployment

### Cloudflare Tunnel

This Spring Boot version is specifically optimized for Cloudflare tunnel compatibility:

```bash
# Install cloudflared
brew install cloudflared

# Authenticate
cloudflared tunnel login

# Create tunnel
cloudflared tunnel create standup-app

# Configure tunnel (in ~/.cloudflared/config.yml)
tunnel: <tunnel-id>
credentials-file: /path/to/credentials.json
ingress:
  - hostname: standup.yourdomain.com
    service: http://localhost:5000
  - service: http_status:404

# Run tunnel
cloudflared tunnel run standup-app
```

### Production Deployment

1. **Create your environment file**:
   ```bash
   cp env.example .env
   # Edit .env with your production credentials
   ```

2. **Deploy with Docker**:
   ```bash
   docker-compose up -d --build
   ```

**Important**: The Dockerfile automatically copies the `.env` file into the container, so ensure your `.env` file contains the correct credentials before building.

3. **Configure reverse proxy** (nginx/Apache)

4. **Set up SSL certificates** (Let's Encrypt)

5. **Configure monitoring** (health checks, logs)

## Monitoring

### Health Checks
- Built-in Spring Boot Actuator health endpoint
- Docker health checks included
- Custom health indicators for file system access

### Log Monitoring
Monitor these log files for operational insights:
- `logs/app.log` - Application events and errors
- `logs/access.log` - HTTP request patterns
- `logs/security.log` - Authentication and security events
- `logs/rate_limit.log` - Rate limiting violations

### Discord Notifications
Automatic notifications for:
- Failed login attempts
- Account lockouts
- Rate limit violations
- Brute force attacks

## Migration from Flask

This Spring Boot application is functionally and visually identical to the original Flask app:

### Preserved Features
- ✅ Exact same authentication flow
- ✅ Identical UI design and styling
- ✅ Same file format and data persistence
- ✅ Matching security features
- ✅ Equivalent rate limiting
- ✅ Same logging format

### Improvements
- ✅ Better Cloudflare tunnel compatibility
- ✅ Enhanced error handling
- ✅ Improved health checks
- ✅ Better container support
- ✅ More robust session management

## 🔒 Security Best Practices

### Credential Management

**⚠️ CRITICAL**: Never commit passwords or API keys to Git!

1. **Use environment variables only**:
   - Store credentials in `.env` file (already in `.gitignore`)
   - Use `env.example` as a template
   - Set empty defaults in `application.properties`

2. **Verify before committing**:
   ```bash
   # Check what Git will commit
   git status
   git diff --staged
   
   # Verify sensitive files are ignored
   git check-ignore .env data/Standups.txt
   ```

3. **Production deployment**:
   - Use Docker secrets or Kubernetes secrets
   - Set environment variables in deployment platform
   - Never use default values from `application.properties`

### Secure Configuration

- **Change default credentials** before first use
- **Use strong passwords** (minimum 12 characters)
- **Rotate secrets regularly** (every 90 days)
- **Monitor security logs** for suspicious activity
- **Enable Discord notifications** for real-time alerts

### Files in Git vs. Excluded

**✅ Safe to commit**:
- `application.properties` (with empty defaults)
- `env.example` (template without real values)
- All source code files
- Docker configuration files

**❌ NEVER commit**:
- `.env` (contains real passwords)
- `data/Standups.txt` (contains personal data)
- `logs/` directory (may contain sensitive info)
- Any file with actual passwords or API keys

## Troubleshooting

### Common Issues

1. **Port 5000 already in use**:
   ```bash
   lsof -ti:5000 | xargs kill -9
   ```

2. **Permission denied on data directory**:
   ```bash
   chmod 755 data logs
   ```

3. **Session tokens not working**:
   - Check `FLASK_SECRET_KEY` environment variable
   - Verify system time is correct
   - Check for app restarts (invalidates sessions)

4. **Rate limiting too strict**:
   - Clear browser cookies
   - Wait for rate limit window to reset
   - Check IP address in logs

### Log Analysis

```bash
# Monitor all logs
tail -f logs/*.log

# Security events only
tail -f logs/security.log

# Rate limiting violations
grep "RATE_LIMIT" logs/rate_limit.log

# Recent login attempts
grep "LOGIN" logs/security.log | tail -20
```

## License

This project maintains the same license as the original Flask application.

## Support

For issues and questions:
1. Check the logs in `logs/` directory
2. Verify environment variables are set correctly
3. Test with curl commands for API debugging
4. Check Docker container health status

---

**The application is now ready for production deployment with enhanced security and Cloudflare tunnel compatibility!** 🚀
