#!/bin/bash

# Script to generate optimized Dockerfiles for all microservices
# This ensures consistency and applies best practices across all services

set -e

# Define services and their ports
declare -A SERVICES=(
    ["auth-service"]="8081"
    ["transaction-service"]="8082"
    ["ocr-service"]="8083"
    ["budget-service"]="8084"
    ["notification-service"]="8085"
    ["gateway-service"]="8080"
    ["config-server"]="8888"
)

TEMPLATE_FILE="Dockerfile.optimized.template"
BACKUP_SUFFIX=".backup.$(date +%Y%m%d_%H%M%S)"

echo "🚀 Generating optimized Dockerfiles for all services..."

# Check if template exists
if [[ ! -f "$TEMPLATE_FILE" ]]; then
    echo "❌ Template file $TEMPLATE_FILE not found!"
    exit 1
fi

# Generate Dockerfile for each service
for service in "${!SERVICES[@]}"; do
    port="${SERVICES[$service]}"
    dockerfile_path="${service}/Dockerfile"
    
    echo "📦 Processing $service (port: $port)..."
    
    # Backup existing Dockerfile if it exists
    if [[ -f "$dockerfile_path" ]]; then
        echo "   📋 Backing up existing Dockerfile to ${dockerfile_path}${BACKUP_SUFFIX}"
        cp "$dockerfile_path" "${dockerfile_path}${BACKUP_SUFFIX}"
    fi
    
    # Generate new Dockerfile from template
    sed -e "s/{{SERVICE_NAME}}/$service/g" \
        -e "s/{{PORT}}/$port/g" \
        "$TEMPLATE_FILE" > "$dockerfile_path"
    
    echo "   ✅ Generated optimized Dockerfile for $service"
done

echo ""
echo "🎉 All Dockerfiles have been generated successfully!"
echo ""
echo "📋 Summary:"
for service in "${!SERVICES[@]}"; do
    echo "   • $service: port ${SERVICES[$service]}"
done

echo ""
echo "🔧 Next steps:"
echo "   1. Review the generated Dockerfiles"
echo "   2. Enable BuildKit: export DOCKER_BUILDKIT=1"
echo "   3. Build with: docker-compose build"
echo "   4. Or use parallel build: ./build-optimized.sh"

echo ""
echo "💡 Pro tips:"
echo "   • First build will take 40-60s (downloading dependencies)"
echo "   • Subsequent builds will be much faster (2-10s with caching)"
echo "   • Change only source code to benefit from dependency caching"
echo "   • Use 'docker system prune -f' to clean up build cache if needed"
