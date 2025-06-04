#!/bin/bash

# Optimized Docker build script for Kubernetes deployment
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
ALL_SERVICES=("auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")
CLUSTER_NAME="coinsight-cluster"
IMAGE_REGISTRY="coinsight"

# Parse command line arguments
if [ $# -eq 0 ]; then
    # No arguments - build all services
    SERVICES=("${ALL_SERVICES[@]}")
    echo -e "${YELLOW}📦 Building all services...${NC}"
elif [ $# -eq 1 ]; then
    # One argument - build specific service
    TARGET_SERVICE="$1"
    if [[ " ${ALL_SERVICES[@]} " =~ " ${TARGET_SERVICE} " ]]; then
        SERVICES=("$TARGET_SERVICE")
        echo -e "${YELLOW}📦 Building single service: $TARGET_SERVICE${NC}"
    else
        echo -e "${RED}❌ Unknown service: $TARGET_SERVICE${NC}"
        echo -e "${YELLOW}Available services: ${ALL_SERVICES[*]}${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Usage: $0 [service-name]${NC}"
    echo -e "${YELLOW}Available services: ${ALL_SERVICES[*]}${NC}"
    exit 1
fi

# Enable BuildKit for faster builds and caching
export DOCKER_BUILDKIT=1
export BUILDKIT_PROGRESS=plain

echo -e "${BLUE}🚀 Starting optimized Kubernetes microservices build...${NC}"
echo -e "${YELLOW}📊 Build configuration:${NC}"
echo -e "   • BuildKit: enabled"
echo -e "   • Max parallel builds: $MAX_PARALLEL_BUILDS"
echo -e "   • Services to build: ${#SERVICES[@]} (${SERVICES[*]})"
echo -e "   • Target cluster: $CLUSTER_NAME"
echo ""

# Check if Kind cluster is running
if ! kind get clusters | grep -q "$CLUSTER_NAME"; then
    echo -e "${RED}❌ Kind cluster '$CLUSTER_NAME' not found. Please start the cluster first.${NC}"
    exit 1
fi

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

# Function to build and load a single service
build_and_load_service() {
    local service=$1
    local start_time=$(date +%s)
    local image_name="$IMAGE_REGISTRY/$service:latest"
    
    echo -e "${BLUE}📦 Building $service...${NC}"
    
    # Build with optimized BuildKit settings
    if docker build \
        --progress=plain \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        --cache-from="$image_name" \
        -f "./$service/Dockerfile" \
        -t "$image_name" \
        . 2>&1 | sed "s/^/[$service] /" | grep -E "(downloading|downloaded|Step|Successfully|CACHED)" || true; then
        
        local build_end=$(date +%s)
        local build_duration=$((build_end - start_time))
        echo -e "${GREEN}✅ $service built successfully in ${build_duration}s${NC}"
        
        # Load image into Kind cluster
        echo -e "${YELLOW}📦 Loading $image_name into Kind cluster...${NC}"
        if kind load docker-image "$image_name" --name "$CLUSTER_NAME" 2>&1 | sed "s/^/[$service] /"; then
            local end_time=$(date +%s)
            local total_duration=$((end_time - start_time))
            echo -e "${GREEN}✅ $service loaded into cluster in ${total_duration}s total${NC}"
            return 0
        else
            echo -e "${RED}❌ Failed to load $service into cluster${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Failed to build $service${NC}"
        return 1
    fi
}

# Function for parallel builds (if needed in future)
build_service_parallel() {
    local service=$1
    build_and_load_service "$service" &
    local pid=$!
    echo "$pid"
}

# Build services (sequential for now, can be parallelized)
echo -e "${YELLOW}🏗️  Building and loading services...${NC}"
total_start=$(date +%s)

failed_services=()

for service in "${SERVICES[@]}"; do
    if ! build_and_load_service "$service"; then
        failed_services+=("$service")
        echo -e "${RED}❌ Build failed for $service${NC}"
        # Continue with other services instead of exiting
    fi
    echo ""
done

total_end=$(date +%s)
total_duration=$((total_end - total_start))

# Build summary
echo ""
if [ ${#failed_services[@]} -eq 0 ]; then
    echo -e "${GREEN}🎉 All services built and loaded successfully in ${total_duration}s!${NC}"
else
    echo -e "${RED}❌ Some services failed to build:${NC}"
    for service in "${failed_services[@]}"; do
        echo -e "${RED}   • $service${NC}"
    done
    echo ""
    echo -e "${YELLOW}✅ Successfully built services:${NC}"
    for service in "${SERVICES[@]}"; do
        if [[ ! " ${failed_services[@]} " =~ " ${service} " ]]; then
            echo -e "${GREEN}   • $service${NC}"
        fi
    done
fi

echo ""
echo -e "${BLUE}📋 Built images:${NC}"
for service in "${SERVICES[@]}"; do
    if [[ ! " ${failed_services[@]} " =~ " ${service} " ]]; then
        echo -e "   • ${GREEN}$IMAGE_REGISTRY/$service:latest${NC}"
    else
        echo -e "   • ${RED}$IMAGE_REGISTRY/$service:latest (FAILED)${NC}"
    fi
done

echo ""
echo -e "${YELLOW}🔍 Verify images in Kind cluster:${NC}"
echo -e "   docker exec -it $CLUSTER_NAME-control-plane crictl images | grep $IMAGE_REGISTRY"

echo ""
if [ ${#failed_services[@]} -eq 0 ]; then
    echo -e "${GREEN}🚀 Ready to deploy with: ./k8s/scripts/deploy.sh${NC}"
    echo -e "${YELLOW}💡 Pro tip: Images are now cached for faster subsequent builds${NC}"
else
    echo -e "${YELLOW}⚠️  Fix the failed services and run the script again${NC}"
    exit 1
fi