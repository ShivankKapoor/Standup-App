#!/bin/bash

# Standup App - Stop and Cleanup Script (using Podman)
# This script stops the container, removes it, and deletes the built image

set -e  # Exit on any error

echo "🛑 Stopping Standup App container..."
podman stop standup-container 2>/dev/null || echo "⚠️  Container was not running"

echo "🗑️  Removing container..."
podman rm standup-container 2>/dev/null || echo "⚠️  Container was already removed"

echo "🧹 Removing built image..."
podman rmi standup-app 2>/dev/null || echo "⚠️  Image was already removed"

echo "✅ Cleanup complete!"
echo "📝 All containers and images for Standup App have been removed"
echo "🚀 To rebuild and run again, use: ./run.sh"
