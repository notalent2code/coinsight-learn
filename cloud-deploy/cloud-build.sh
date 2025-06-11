#!/bin/bash

# Cloud Docker Build Script for Coinsight Microservices
# Works with Google Cloud Build, AWS CodeBuild, or any CI/CD system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
CLOUD_PROVIDER="gcp"  # gcp, aws, or docker-hub
PROJECT_ID=""
REGISTRY_URL=""
ALL_SERVICES=("auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")

# Function to show usage
show_usage() {
    echo -e "${YELLOW}Usage: $0 [CLOUD_PROVIDER] [PROJECT_ID]${NC}"
    echo ""
    echo -e "${YELLOW}CLOUD_PROVIDER:${NC}"
    echo -e "  gcp         Google Cloud Platform (Artifact Registry)"
    echo -e "  aws         Amazon Web Services (ECR)"
    echo -e "  docker-hub  Docker Hub"
    echo ""
    echo -e "${YELLOW}Examples:${NC}"
    echo -e "  $0 gcp my-project-id"
    echo -e "  $0 aws 123456789012"
    echo -e "  $0 docker-hub myusername"
}

# Setup registry URL based on cloud provider
setup_registry() {
    case $CLOUD_PROVIDER in
        gcp)
            if [ -z "$PROJECT_ID" ]; then
                echo -e "${RED}❌ Project ID required for GCP${NC}"
                exit 1
            fi
            REGISTRY_URL="asia-southeast2-docker.pkg.dev/${PROJECT_ID}/coinsight-registry"
            echo -e "${BLUE}🔧 Using GCP Artifact Registry: $REGISTRY_URL${NC}"
            # Authenticate with GCP
            gcloud auth configure-docker asia-southeast2-docker.pkg.dev
            ;;
        aws)
            if [ -z "$PROJECT_ID" ]; then
                echo -e "${RED}❌ AWS Account ID required${NC}"
                exit 1
            fi
            REGISTRY_URL="${PROJECT_ID}.dkr.ecr.ap-southeast-1.amazonaws.com/coinsight"
            echo -e "${BLUE}🔧 Using AWS ECR: $REGISTRY_URL${NC}"
            # Authenticate with AWS ECR
            aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin $REGISTRY_URL
            ;;
        docker-hub)
            if [ -z "$PROJECT_ID" ]; then
                echo -e "${RED}❌ Docker Hub username required${NC}"
                exit 1
            fi
            REGISTRY_URL="docker.io/${PROJECT_ID}/coinsight"
            echo -e "${BLUE}🔧 Using Docker Hub: $REGISTRY_URL${NC}"
            echo -e "${YELLOW}💡 Make sure you're logged in: docker login${NC}"
            ;;
        *)
            echo -e "${RED}❌ Unsupported cloud provider: $CLOUD_PROVIDER${NC}"
            show_usage
            exit 1
            ;;
    esac
}

# Build all services
build_services() {
    echo -e "${BLUE}🔨 Building Coinsight microservices...${NC}"
    
    for service in "${ALL_SERVICES[@]}"; do
        echo -e "${YELLOW}📦 Building $service...${NC}"
        
        # Build image with proper tagging
        docker build \
            -f "$service/Dockerfile" \
            -t "${REGISTRY_URL}-${service}:latest" \
            -t "${REGISTRY_URL}-${service}:$(date +%Y%m%d-%H%M%S)" \
            .
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ $service built successfully${NC}"
        else
            echo -e "${RED}❌ Failed to build $service${NC}"
            exit 1
        fi
    done
}

# Push all services
push_services() {
    echo -e "${BLUE}🚀 Pushing images to registry...${NC}"
    
    for service in "${ALL_SERVICES[@]}"; do
        echo -e "${YELLOW}⬆️  Pushing $service...${NC}"
        
        # Push both latest and timestamped tags
        docker push "${REGISTRY_URL}-${service}:latest"
        docker push "${REGISTRY_URL}-${service}:$(date +%Y%m%d-%H%M%S)" || echo "⚠️  Timestamp tag push failed (normal for concurrent builds)"
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ $service pushed successfully${NC}"
        else
            echo -e "${RED}❌ Failed to push $service${NC}"
            exit 1
        fi
    done
}

# Generate deployment values file
generate_deployment_values() {
    echo -e "${BLUE}📝 Generating deployment values...${NC}"
    
    cat > cloud-deploy/cloud-values.yaml << EOF
# Cloud deployment values for Coinsight Platform
global:
  namespace: "coinsight"
  image:
    registry: "${REGISTRY_URL}"
    pullPolicy: Always
    tag: "latest"

microservices:
  authService:
    image:
      repository: "${REGISTRY_URL}-auth-service"
  transactionService:
    image:
      repository: "${REGISTRY_URL}-transaction-service"
  ocrService:
    image:
      repository: "${REGISTRY_URL}-ocr-service"
  budgetService:
    image:
      repository: "${REGISTRY_URL}-budget-service"
  notificationService:
    image:
      repository: "${REGISTRY_URL}-notification-service"
  gatewayService:
    image:
      repository: "${REGISTRY_URL}-gateway-service"
    service:
      type: LoadBalancer
      port: 8080

# Cloud-optimized persistence
postgres-auth:
  primary:
    persistence:
      size: 10Gi
postgres-transaction:
  primary:
    persistence:
      size: 20Gi
postgres-budget:
  primary:
    persistence:
      size: 10Gi
postgres-notification:
  primary:
    persistence:
      size: 10Gi
postgres-keycloak:
  primary:
    persistence:
      size: 10Gi

kafka:
  broker:
    persistence:
      size: 20Gi
  zookeeper:
    persistence:
      size: 10Gi

redis:
  master:
    persistence:
      size: 8Gi
EOF

    echo -e "${GREEN}✅ Deployment values created: cloud-deploy/cloud-values.yaml${NC}"
}

# Main execution
main() {
    # Parse arguments
    if [ $# -lt 1 ]; then
        show_usage
        exit 1
    fi
    
    CLOUD_PROVIDER=$1
    PROJECT_ID=$2
    
    echo -e "${BLUE}🚀 Coinsight Cloud Build Script${NC}"
    echo -e "${YELLOW}==============================${NC}"
    
    # Create cloud-deploy directory
    mkdir -p cloud-deploy
    
    # Setup registry
    setup_registry
    
    # Build and push
    build_services
    push_services
    
    # Generate deployment values
    generate_deployment_values
    
    echo ""
    echo -e "${GREEN}🎉 Build and push completed successfully!${NC}"
    echo ""
    echo -e "${BLUE}📋 Next steps:${NC}"
    echo -e "   1. Deploy to your cloud K8s cluster"
    echo -e "   2. Use the generated values file: cloud-deploy/cloud-values.yaml"
    echo ""
    echo -e "${BLUE}🔗 Built images:${NC}"
    for service in "${ALL_SERVICES[@]}"; do
        echo -e "   • ${REGISTRY_URL}-${service}:latest"
    done
}

# Run main function
main "$@"
