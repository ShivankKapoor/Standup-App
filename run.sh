#!/bin/bash

# Standup App - Build and Run Script (Docker/Podman + Cloudflare Tunnel)
# This script builds the image and runs the container with proper CF Tunnel support

set -e  # Exit on any error

# Detect container runtime (Docker or Podman)
if command -v docker &> /dev/null; then
    CONTAINER_CMD="docker"
elif command -v podman &> /dev/null; then
    CONTAINER_CMD="podman"
else
    echo "❌ Neither Docker nor Podman found. Please install one of them."
    exit 1
fi

echo "🐳 Using $CONTAINER_CMD as container runtime"
echo "🏗️  Building Standup App image..."
$CONTAINER_CMD build -t standup-app .

echo "🛑 Stopping existing container (if running)..."
$CONTAINER_CMD stop standup-container 2>/dev/null || true

echo "🗑️  Removing existing container (if exists)..."
$CONTAINER_CMD rm standup-container 2>/dev/null || true

echo "🚀 Starting new container optimized for Cloudflare Tunnel..."
$CONTAINER_CMD run -d \
    -p 5555:5555 \
    -v "$(pwd)/data:/app/data" \
    -v "$(pwd)/logs:/app/logs" \
    --env-file .env \
    --name standup-container \
    standup-app

echo "✅ Standup App is now running!"
echo "📱 Local access: http://localhost:5555"
echo "🌐 For Cloudflare Tunnel, ensure tunnel-config.yml points to localhost:5555"
echo "📊 To view logs: $CONTAINER_CMD logs -f standup-container"
echo "🛑 To stop: $CONTAINER_CMD stop standup-container"
echo ""
echo "🔧 Quick commands:"
echo "   Health check: curl http://localhost:5555/health"
echo "   View logs: $CONTAINER_CMD logs standup-container"
echo "   Restart: $CONTAINER_CMD restart standup-container"
