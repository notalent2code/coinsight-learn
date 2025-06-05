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

# Default build mode
BUILD_MODE="cached"  # cached or fresh
SERVICES=()

# Function to show usage
show_usage() {
    echo -e "${YELLOW}Usage: $0 [OPTIONS] [SERVICE_NAME]${NC}"
    echo ""
    echo -e "${YELLOW}OPTIONS:${NC}"
    echo -e "  --fresh, -f     Fresh build (no cache, force rebuild)"
    echo -e "  --cached, -c    Cached build (default, use existing cache)"
    echo -e "  --help, -h      Show this help message"
    echo ""
    echo -e "${YELLOW}SERVICE_NAME:${NC}"
    echo -e "  Specific service to build (optional)"
    echo -e "  Available services: ${ALL_SERVICES[*]}"
    echo ""
    echo -e "${YELLOW}Examples:${NC}"
    echo -e "  $0                    # Build all services with cache"
    echo -e "  $0 --fresh            # Fresh build all services"
    echo -e "  $0 -c transaction-service    # Cached build transaction-service"
    echo -e "  $0 --fresh budget-service    # Fresh build budget-service"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --fresh|-f)
            BUILD_MODE="fresh"
            shift
            ;;
        --cached|-c)
            BUILD_MODE="cached"
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        -*)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
        *)
            # This should be a service name
            if [[ " ${ALL_SERVICES[@]} " =~ " $1 " ]]; then
                SERVICES=("$1")
            else
                echo -e "${RED}❌ Unknown service: $1${NC}"
                echo -e "${YELLOW}Available services: ${ALL_SERVICES[*]}${NC}"
                exit 1
            fi
            shift
            ;;
    esac
done

# If no specific service provided, build all services
if [ ${#SERVICES[@]} -eq 0 ]; then
    SERVICES=("${ALL_SERVICES[@]}")
fi

# Display build configuration
if [ ${#SERVICES[@]} -eq ${#ALL_SERVICES[@]} ]; then
    echo -e "${YELLOW}📦 Building all services with ${BUILD_MODE} mode...${NC}"
else
    echo -e "${YELLOW}📦 Building service '${SERVICES[0]}' with ${BUILD_MODE} mode...${NC}"
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
    
    echo -e "${BLUE}📦 Building $service (${BUILD_MODE} mode)...${NC}"
    echo -e "${YELLOW}   • Image: $image_name${NC}"
    echo -e "${YELLOW}   • Dockerfile: ./$service/Dockerfile${NC}"
    
    # Determine build arguments based on mode
    local build_args=""
    if [ "$BUILD_MODE" = "fresh" ]; then
        echo -e "${YELLOW}   • Mode: Fresh build (no cache)${NC}"
        build_args="--no-cache --pull"
    else
        echo -e "${YELLOW}   • Mode: Cached build${NC}"
        build_args="--build-arg BUILDKIT_INLINE_CACHE=1 --cache-from=$image_name"
    fi
    
    echo -e "${BLUE}   🔨 Starting Docker build...${NC}"
    
    # Build with verbose output and real-time progress
    if docker build \
        --progress=plain \
        $build_args \
        -f "./$service/Dockerfile" \
        -t "$image_name" \
        . 2>&1 | while IFS= read -r line; do
            # Show all output with service prefix and timestamp
            timestamp=$(date '+%H:%M:%S')
            case "$line" in
                *"Step "*)
                    echo -e "${BLUE}[$service $timestamp] $line${NC}"
                    ;;
                *"CACHED"*)
                    echo -e "${GREEN}[$service $timestamp] $line${NC}"
                    ;;
                *"downloading"*|*"downloaded"*|*"Downloading"*|*"Downloaded"*)
                    echo -e "${YELLOW}[$service $timestamp] $line${NC}"
                    ;;
                *"Successfully"*)
                    echo -e "${GREEN}[$service $timestamp] $line${NC}"
                    ;;
                *"ERROR"*|*"Error"*|*"error"*)
                    echo -e "${RED}[$service $timestamp] $line${NC}"
                    ;;
                *)
                    echo "[$service $timestamp] $line"
                    ;;
            esac
        done; then
        
        local build_end=$(date +%s)
        local build_duration=$((build_end - start_time))
        echo -e "${GREEN}✅ $service built successfully in ${build_duration}s${NC}"
        
        # Load image into Kind cluster with verbose output
        echo -e "${YELLOW}📦 Loading $image_name into Kind cluster...${NC}"
        echo -e "${YELLOW}   • Cluster: $CLUSTER_NAME${NC}"
        echo -e "${YELLOW}   • This may take a moment...${NC}"
        
        if kind load docker-image "$image_name" --name "$CLUSTER_NAME" 2>&1 | while IFS= read -r line; do
            timestamp=$(date '+%H:%M:%S')
            echo "[$service $timestamp] $line"
        done; then
            local end_time=$(date +%s)
            local total_duration=$((end_time - start_time))
            echo -e "${GREEN}✅ $service loaded into cluster in ${total_duration}s total${NC}"
            echo -e "${GREEN}   • Build: ${build_duration}s${NC}"
            echo -e "${GREEN}   • Load: $((total_duration - build_duration))s${NC}"
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