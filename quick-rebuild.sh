#!/bin/bash

# Quick rebuild script for development
# Usage: ./quick-rebuild.sh [service-name]

# Enable BuildKit
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

SERVICE=$1

if [ -z "$SERVICE" ]; then
    echo "Usage: ./quick-rebuild.sh [service-name]"
    echo "Available services:"
    echo "  - config-server"
    echo "  - auth-service" 
    echo "  - transaction-service"
    echo "  - budget-service"
    echo "  - notification-service"
    echo "  - ocr-service"
    echo "  - gateway-service"
    exit 1
fi

echo "🔄 Quick rebuilding $SERVICE..."

# Stop the service
docker-compose stop $SERVICE

# Rebuild with cache
docker-compose build $SERVICE

# Start the service
docker-compose up -d $SERVICE

echo "✅ $SERVICE rebuilt and restarted!"

# Show logs
echo "📄 Showing logs for $SERVICE (Ctrl+C to exit):"
docker-compose logs -f $SERVICE
