# 📋 Standup App

A self-hosted Spring Boot application for tracking daily standup notes. Features an interactive calendar UI, activity heatmap, two-factor authentication, Discord notifications, and Cloudflare Tunnel support.

**Tech Stack:** Java 17 · Spring Boot 3.5 · Kotlin · Thymeleaf · Docker · Maven

## Features

- **Interactive Calendar** — Month-by-month view with color-coded dots for entry types (regular, PTO, planning, support)
- **Activity Heatmap** — GitHub-style contribution graph with streak tracking, word count stats, and configurable time ranges
- **Two-Factor Authentication** — TOTP-based 2FA via Google Authenticator (skippable in QA mode)
- **Session Management** — HMAC-SHA256 signed tokens with configurable timeout (default 15 min), live countdown timer
- **Rate Limiting** — Per-IP and per-user limits on authentication, API, and admin endpoints
- **Discord Integration** — Webhook notifications for security events, login failures, and uptime alerts
- **Scheduled Tasks** — Automatic session cleanup (daily at midnight CST) and uptime monitoring (daily at 7 PM CST)
- **Weather Widget** — Client-side geolocation with Open-Meteo API, cached for 30 minutes
- **File-Based Storage** — Flat-file persistence in `data/Standups.txt`, no database required
- **Cloudflare Tunnel Ready** — Proxy header handling, IP resolution through forwarded headers

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+ (or use the included `mvnw` wrapper)
- Docker & Docker Compose (for containerized deployment)

### 1. Configure Environment

```bash
cp env.example .env
```

Edit `.env` with your credentials:

```bash
AUTH_USERNAME=your_username
AUTH_PASSWORD=your_secure_password
FLASK_SECRET_KEY=your_hmac_signing_key
AUTH_2FA_SECRET=your_totp_secret
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

### 2a. Run with Docker (Recommended)

```bash
docker-compose up --build
# App available at http://localhost:5555
```

Or use the included helper scripts:

```bash
./run.sh    # Build and start container
./stop.sh   # Stop and remove container
```

### 2b. Run Locally

```bash
./mvnw clean install
./mvnw spring-boot:run
# App available at http://localhost:5555
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MODE` | `PROD` | Set to `QA` to disable 2FA and Discord notifications |
| `AUTH_USERNAME` | *(required)* | Login username |
| `AUTH_PASSWORD` | *(required)* | Login password |
| `FLASK_SECRET_KEY` | *(required)* | HMAC signing key for session tokens |
| `AUTH_2FA_SECRET` | *(none)* | TOTP secret for Google Authenticator |
| `SESSION_TIMEOUT_SECONDS` | `900` | Session timeout in seconds (15 minutes) |
| `WTF_CSRF_TIME_LIMIT` | `3600` | CSRF token lifetime in seconds |
| `STANDUPS_FILE` | `data/Standups.txt` | Path to the data file |
| `DISCORD_WEBHOOK_URL` | *(none)* | Discord webhook for notifications |
| `APP_UPTIME_MAX_MILLIS` | `604800000` | Uptime alert threshold (default 7 days) |
| `SERVER_PORT` | `5555` | Application port |

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Calendar view |
| `/login` | GET/POST | Authentication |
| `/2fa` | GET/POST | Two-factor verification |
| `/logout` | GET | End session |
| `/entry/{date}` | GET | View standup entry |
| `/edit/{date}` | GET | Edit standup entry |
| `/save/{date}` | POST | Save standup entry |
| `/delete/{date}` | POST | Delete standup entry |
| `/health` | GET | Health check with system info |
| `/api/heatmap` | GET | Heatmap data (`?daysBack=365`) |
| `/api/uptime/check` | GET | Current uptime status |
| `/api/uptime/trigger-check` | POST | Trigger uptime check with Discord alert |
| `/admin/cleanup-sessions` | GET | Manual session cleanup |

## Authentication Flow

1. User submits credentials at `/login`
2. On success, redirected to `/2fa` for TOTP verification (skipped in QA mode)
3. Valid 2FA code creates an HMAC-SHA256 signed session token stored as a cookie
4. All protected routes validate the token via `AuthenticationInterceptor`
5. Sessions expire after 15 minutes of inactivity; tokens are invalidated on app restart

## Security

| Feature | Details |
|---------|---------|
| **Authentication** | Username/password + TOTP 2FA |
| **Session Tokens** | HMAC-SHA256 signed, time-limited, instance-bound |
| **Rate Limiting** | Auth: 5/hr per IP · API: 60/hr per user · Health: 10/hr · Uptime: 20/hr |
| **Account Lockout** | 1 hour after 5 failed login attempts |
| **Input Validation** | 50KB limit, date format validation |
| **CSRF Protection** | Token-based with 1-hour lifetime |
| **Cookies** | HttpOnly, SameSite=Strict |

### Logging

Four dedicated log files under `logs/`:

| File | Contents |
|------|----------|
| `app.log` | Application events |
| `access.log` | HTTP request logs |
| `security.log` | Auth attempts, lockouts, brute force detection |
| `rate_limit.log` | Rate limit violations |

Security events are also sent to Discord as rich embeds.

## Data Format

Entries are stored in `data/Standups.txt`:

```
9_10_24 
Yesterday I worked on writing the Wiki for Synergy setup.
Today looking for another US to work on.
-------------------------------------------------------------------

$(PTO)
9_11_24 
PTO - vacation day
-------------------------------------------------------------------
```

- **Date**: `M_D_YY` format, converted internally to `YYYY-MM-DD`
- **Day types**: Prefix with `$(PTO)`, `$(Planning)`, or `$(Support)` for special entries
- **Separator**: Dashes mark the end of each entry

## Project Structure

```
src/main/
├── java/com/shivank/Standup_App/
│   ├── StandupAppApplication.java
│   ├── config/
│   │   ├── EnvironmentConfig.java        # .env file loader
│   │   └── WebConfig.java                # MVC configuration
│   ├── controller/
│   │   ├── MainController.java           # Calendar, login, entry CRUD
│   │   ├── HealthController.java         # /health endpoint
│   │   ├── HeatmapController.kt          # /api/heatmap endpoint
│   │   └── UptimeController.java         # /api/uptime endpoints
│   ├── interceptor/
│   │   └── AuthenticationInterceptor.java # Session validation
│   └── service/
│       ├── CalendarService.java           # Calendar grid generation
│       ├── DiscordService.java            # Webhook messaging
│       ├── HeatmapService.kt             # Activity stats & heatmap data
│       ├── IpAddressService.java          # Client IP resolution
│       ├── LoggingService.java            # Structured logging & Discord alerts
│       ├── RateLimitService.java          # Per-IP/user rate limiting
│       ├── ScheduledTaskService.java      # Cron jobs (cleanup, uptime)
│       ├── SessionService.java            # HMAC token management
│       ├── StandupDataService.java        # File I/O for entries
│       └── TwoFactorService.java          # TOTP validation
└── resources/
    ├── application.properties
    ├── static/css/theme.css
    └── templates/
        ├── index.html                     # Calendar view
        ├── login.html                     # Login page
        ├── 2fa.html                       # 2FA verification
        ├── entry.html                     # View entry
        ├── edit.html                      # Edit entry
        └── logout.html                    # Logout confirmation
```

## Deployment

### Docker

```bash
# Build and run
docker-compose up -d --build

# View logs
docker logs -f standup-app

# Health check
curl http://localhost:5555/health
```

The `.env` file is copied into the Docker image at build time. Volume mounts persist `data/` and `logs/` on the host.

### Cloudflare Tunnel

The app runs on HTTP (port 5555) with SSL termination handled by Cloudflare:

```bash
cloudflared tunnel create standup-app

# In ~/.cloudflared/config.yml:
tunnel: <tunnel-id>
credentials-file: /path/to/credentials.json
ingress:
  - hostname: standup.yourdomain.com
    service: http://localhost:5555
  - service: http_status:404

cloudflared tunnel run standup-app
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 5555 in use | `lsof -ti:5555 \| xargs kill -9` |
| Permission denied on data/logs | `chmod 755 data logs` |
| Sessions not persisting | Check `FLASK_SECRET_KEY` is set; sessions reset on app restart |
| Rate limited | Wait for the hourly window to reset, or check `security.log` |
| 2FA not working | Verify `AUTH_2FA_SECRET` matches your authenticator app; check system clock sync |
