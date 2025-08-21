#!/bin/bash

# Standup App - Build and Run Script (using Podman)
# This script builds the Docker image and runs the container using Podman

set -e  # Exit on any error

echo "🏗️  Building Standup App image with Podman..."
podman build -t standup-app .

echo "🛑 Stopping existing container (if running)..."
podman stop standup-container 2>/dev/null || true

echo "🗑️  Removing existing container (if exists)..."
podman rm standup-container 2>/dev/null || true

echo "🚀 Starting new container..."
podman run -d \
    -p 5555:5555 \
    -v "$(pwd)/data:/app/data" \
    -v "$(pwd)/logs:/app/logs" \
    --name standup-container \
    standup-app

echo "✅ Standup App is now running!"
echo "📱 Access the application at: http://localhost:5555"
echo "📊 To view logs: podman logs -f standup-container"
echo "🛑 To stop: podman stop standup-container"
