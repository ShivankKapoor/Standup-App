# Docker + Cloudflare Tunnel Setup Guide

This guide explains how to run the Standup App with Docker and Cloudflare Tunnel for secure HTTPS access.

## Architecture

```
Internet → Cloudflare (HTTPS) → CF Tunnel → Docker Container (HTTP:5555)
```

- **Cloudflare**: Handles HTTPS termination, SSL certificates, security
- **CF Tunnel**: Secure connection between Cloudflare and your local Docker container
- **Docker Container**: Runs HTTP only (port 5555), no SSL complexity

## Quick Start

### 1. Prerequisites
- Docker or Podman installed
- Cloudflare account with domain
- Cloudflare Tunnel set up

### 2. Environment Setup
Copy `env.example` to `.env` and configure:
```bash
cp env.example .env
# Edit .env with your actual values
```

### 3. Run with Docker/Podman
```bash
# Option A: Use the run script (recommended)
./run.sh

# Option B: Use Docker Compose
docker-compose up -d

# Option C: Manual Docker commands
docker build -t standup-app .
docker run -d -p 5555:5555 -v $(pwd)/data:/app/data --env-file .env --name standup-container standup-app
```

### 4. Start Cloudflare Tunnel
```bash
# Update tunnel-config.yml with your tunnel ID and domain
cloudflared tunnel --config tunnel-config.yml run
```

## Configuration Files

### application.properties
- **SSL disabled**: Cloudflare handles HTTPS
- **Proxy headers**: Properly configured for CF Tunnel
- **Cookies**: Optimized for proxy setup

### tunnel-config.yml
- **HTTP origin**: Points to localhost:5555 (your Docker container)
- **No TLS verification**: Since container runs HTTP
- **Proper headers**: For seamless proxy integration

### Dockerfile
- **Multi-stage build**: Efficient image size
- **Health checks**: HTTP-based monitoring
- **Volume mounts**: Persistent data and logs

## Why This Works Better

1. **Separation of concerns**: 
   - Cloudflare = HTTPS/Security
   - Docker = Application runtime
   - No SSL conflicts

2. **Simplified configuration**:
   - No certificate management in app
   - No SSL/TLS complexity
   - Standard HTTP in container

3. **Production-ready**:
   - Secure by default
   - Easy to scale
   - Proper logging and monitoring

## Troubleshooting

### Container Issues
```bash
# Check container status
docker ps
docker logs standup-container

# Health check
curl http://localhost:5555/health
```

### Tunnel Issues
```bash
# Test tunnel config
cloudflared tunnel --config tunnel-config.yml validate

# Check tunnel logs
cloudflared tunnel --config tunnel-config.yml run --loglevel debug
```

### Common Problems

1. **502 Bad Gateway**: Container not running or port mismatch
2. **SSL errors**: Check CF SSL/TLS settings (should be "Flexible" or "Full")
3. **Session issues**: Clear browser cookies and restart

## Best Practices

- Always use `.env` file for secrets
- Keep data and logs in mounted volumes
- Monitor container health with `/health` endpoint
- Use CF Analytics to monitor tunnel performance
- Regularly update base images for security

## Comparison with visit-log-service

Both services now follow the same pattern:
- HTTP-only containers
- External HTTPS termination
- Simplified configuration
- Cloud-native approach

This makes standup-app as reliable as visit-log-service!
