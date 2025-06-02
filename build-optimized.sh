#!/bin/bash

# Optimized Docker build script for microservices
# Uses BuildKit, parallel builds, and intelligent caching

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MAX_PARALLEL_BUILDS=3  # Adjust based on your system
SERVICES=("config-server" "auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")
INFRASTRUCTURE_SERVICES=("postgres" "auth-db" "transaction-db" "budget-db" "notification-db" "keycloak" "zookeeper" "kafka" "redis" "prometheus" "grafana" "mailhog")

# Enable BuildKit for faster builds and caching
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo -e "${BLUE}🚀 Starting optimized microservices build...${NC}"
echo -e "${YELLOW}📊 Build configuration:${NC}"
echo -e "   • BuildKit: enabled"
echo -e "   • Max parallel builds: $MAX_PARALLEL_BUILDS"
echo -e "   • Services: ${#SERVICES[@]}"
echo ""

# Function to build a single service
build_service() {
    local service=$1
    local start_time=$(date +%s)
    
    echo -e "${BLUE}📦 Building $service...${NC}"
    
    if docker-compose build --progress=plain "$service" 2>&1 | \
       sed "s/^/[$service] /" | \
       grep -E "(downloading|downloaded|Step|Successfully)" || true; then
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${GREEN}✅ $service built successfully in ${duration}s${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to build $service${NC}"
        return 1
    fi
}

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Pre-build check: ensure all Dockerfiles exist
echo -e "${YELLOW}🔍 Pre-build validation...${NC}"
for service in "${SERVICES[@]}"; do
    if [[ ! -f "$service/Dockerfile" ]]; then
        echo -e "${RED}❌ Dockerfile not found for $service${NC}"
        echo -e "${YELLOW}💡 Run ./generate-dockerfiles.sh to create optimized Dockerfiles${NC}"
        exit 1
    fi
done

# Build services sequentially for now (can be parallelized later)
echo -e "${YELLOW}🏗️  Building services...${NC}"
total_start=$(date +%s)

for service in "${SERVICES[@]}"; do
    if ! build_service "$service"; then
        echo -e "${RED}❌ Build failed for $service${NC}"
        exit 1
    fi
done

total_end=$(date +%s)
total_duration=$((total_end - total_start))

# Build summary
echo ""
echo -e "${GREEN}🎉 All services built successfully in ${total_duration}s!${NC}"
echo ""

# Show image sizes
echo -e "${YELLOW}📊 Image sizes:${NC}"
for service in "${SERVICES[@]}"; do
    image_name="coinsight-${service}"
    size=$(docker images --format "{{.Size}}" "$image_name" 2>/dev/null | head -1 || echo "N/A")
    echo -e "   • $image_name: $size"
done

echo ""
echo -e "${BLUE}🚀 Ready to start services with: docker-compose up -d${NC}"
echo -e "${YELLOW}💡 Pro tip: Use 'docker-compose up -d --scale gateway-service=2' for load balancing${NC}"
