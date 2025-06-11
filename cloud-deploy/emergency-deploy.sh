#!/bin/bash

# 🚀 ONE-CLICK COINSIGHT DEPLOYMENT
# Emergency deployment script for urgent presentations
# Automatically detects environment and deploys accordingly

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "🚀 COINSIGHT EMERGENCY DEPLOYMENT"
echo "================================="
echo -e "${NC}"
echo -e "${YELLOW}⚡ For urgent presentations when your main laptop is dead!${NC}"
echo ""

# Detect environment
detect_environment() {
    echo -e "${BLUE}🔍 Detecting deployment environment...${NC}"
    
    # Check if running on Google Cloud Shell
    if [ -n "$GOOGLE_CLOUD_PROJECT" ]; then
        echo -e "${GREEN}✅ Google Cloud Shell detected!${NC}"
        DEPLOY_METHOD="gcp"
        return
    fi
    
    # Check if running on AWS Cloud9
    if [ -n "$AWS_REGION" ]; then
        echo -e "${GREEN}✅ AWS Cloud9 detected!${NC}"
        DEPLOY_METHOD="aws"
        return
    fi
    
    # Check if this looks like a VPS
    if [ -f /etc/os-release ] && grep -q "Ubuntu\|Debian\|CentOS" /etc/os-release; then
        echo -e "${GREEN}✅ VPS/Linux server detected!${NC}"
        DEPLOY_METHOD="vps"
        return
    fi
    
    # Default to local with Docker
    echo -e "${YELLOW}⚠️  Unknown environment, defaulting to local Docker deployment${NC}"
    DEPLOY_METHOD="docker"
}

# Quick GCP deployment
deploy_gcp() {
    echo -e "${BLUE}☁️  Google Cloud deployment starting...${NC}"
    
    # Set up project if needed
    if [ -z "$GOOGLE_CLOUD_PROJECT" ]; then
        echo -e "${YELLOW}📝 Setting up GCP project...${NC}"
        gcloud projects create coinsight-$(date +%s) --name="Coinsight Demo"
        export GOOGLE_CLOUD_PROJECT=$(gcloud config get-value project)
    fi
    
    # Enable APIs quickly
    gcloud services enable container.googleapis.com artifactregistry.googleapis.com --async
    
    echo -e "${BLUE}🏗️  Creating GKE cluster (this takes ~10 minutes)...${NC}"
    gcloud container clusters create-auto coinsight-demo \
        --region=asia-southeast2 \
        --async
    
    echo -e "${YELLOW}⏳ While cluster creates, building images...${NC}"
    
    # Build images using Cloud Build
    for service in auth-service transaction-service gateway-service budget-service notification-service ocr-service; do
        gcloud builds submit --tag gcr.io/$GOOGLE_CLOUD_PROJECT/$service:latest \
            --file $service/Dockerfile . --async &
    done
    
    # Wait for cluster
    echo -e "${BLUE}⏳ Waiting for cluster to be ready...${NC}"
    gcloud container clusters get-credentials coinsight-demo --region=asia-southeast2
    
    # Quick deploy
    echo -e "${BLUE}🚀 Deploying services...${NC}"
    kubectl create namespace coinsight
    
    # Deploy using simplified manifests
    deploy_simple_manifests
    
    # Get external IP
    GATEWAY_IP=$(kubectl get service gateway-service -n coinsight -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    echo -e "${GREEN}🎉 Deployment complete!${NC}"
    echo -e "${BLUE}🔗 Access URL: http://${GATEWAY_IP}:8080${NC}"
}

# Quick VPS deployment
deploy_vps() {
    echo -e "${BLUE}🖥️  VPS deployment starting...${NC}"
    
    # Quick Docker install
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}📦 Installing Docker quickly...${NC}"
        curl -fsSL https://get.docker.com | sh
        sudo usermod -aG docker $USER
        sudo systemctl start docker
    fi
    
    # Install Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    fi
    
    echo -e "${BLUE}🔨 Building images...${NC}"
    
    # Quick build all services
    for service in auth-service transaction-service gateway-service budget-service notification-service ocr-service; do
        docker build -f $service/Dockerfile -t coinsight/$service:latest . &
    done
    wait
    
    echo -e "${BLUE}🚀 Starting services with Docker Compose...${NC}"
    
    # Create simplified docker-compose
    create_docker_compose
    docker-compose up -d
    
    # Get VPS IP
    VPS_IP=$(curl -s ifconfig.me)
    echo -e "${GREEN}🎉 Deployment complete!${NC}"
    echo -e "${BLUE}🔗 Access URL: http://${VPS_IP}:8080${NC}"
}

# Simple Docker deployment
deploy_docker() {
    echo -e "${BLUE}🐳 Docker deployment starting...${NC}"
    
    create_docker_compose
    docker-compose up -d --build
    
    echo -e "${GREEN}🎉 Deployment complete!${NC}"
    echo -e "${BLUE}🔗 Access URL: http://localhost:8080${NC}"
}

# Create simplified Kubernetes manifests
deploy_simple_manifests() {
    cat << 'EOF' | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
  namespace: coinsight
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gateway-service
  template:
    metadata:
      labels:
        app: gateway-service
    spec:
      containers:
      - name: gateway-service
        image: gcr.io/$GOOGLE_CLOUD_PROJECT/gateway-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "demo"
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
  namespace: coinsight
spec:
  type: LoadBalancer
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: gateway-service
EOF
}

# Create simplified Docker Compose
create_docker_compose() {
    cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: coinsight
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  keycloak:
    image: quay.io/keycloak/keycloak:22.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/coinsight
      KC_DB_USERNAME: postgres
      KC_DB_PASSWORD: postgres
    ports:
      - "8090:8080"
    depends_on:
      - postgres
    command: start-dev

  gateway-service:
    build:
      context: .
      dockerfile: gateway-service/Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - kafka
      - keycloak
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      REDIS_HOST: redis
      KAFKA_SERVERS: kafka:9092
      KEYCLOAK_URL: http://keycloak:8080

  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - keycloak
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      KEYCLOAK_URL: http://keycloak:8080

  transaction-service:
    build:
      context: .
      dockerfile: transaction-service/Dockerfile
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - kafka
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      REDIS_HOST: redis
      KAFKA_SERVERS: kafka:9092

  budget-service:
    build:
      context: .
      dockerfile: budget-service/Dockerfile
    ports:
      - "8084:8084"
    depends_on:
      - postgres
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      KAFKA_SERVERS: kafka:9092

volumes:
  postgres_data:
EOF
}

# Main execution
main() {
    echo -e "${YELLOW}🎯 Emergency deployment for presentation in progress...${NC}"
    echo ""
    
    # Quick confirmation
    echo -e "${BLUE}This will:${NC}"
    echo -e "  ✅ Detect your environment automatically"
    echo -e "  ✅ Deploy Coinsight microservices"
    echo -e "  ✅ Provide access URL in ~10-15 minutes"
    echo ""
    read -p "Continue? (Y/n): " confirm
    if [[ $confirm =~ ^[Nn]$ ]]; then
        echo -e "${YELLOW}Deployment cancelled${NC}"
        exit 0
    fi
    
    # Detect and deploy
    detect_environment
    
    case $DEPLOY_METHOD in
        gcp)
            deploy_gcp
            ;;
        aws)
            echo -e "${YELLOW}AWS deployment coming soon! Using Docker instead...${NC}"
            deploy_docker
            ;;
        vps)
            deploy_vps
            ;;
        docker)
            deploy_docker
            ;;
    esac
    
    echo ""
    echo -e "${GREEN}🎉 DEPLOYMENT COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${YELLOW}========================================${NC}"
    echo ""
    echo -e "${BLUE}💡 Quick health check:${NC}"
    echo -e "   curl http://YOUR_IP:8080/actuator/health"
    echo ""
    echo -e "${BLUE}🎯 Ready for your presentation!${NC}"
    echo -e "${YELLOW}Total time: ~10-15 minutes${NC}"
}

# Error handling
trap 'echo -e "${RED}❌ Emergency deployment failed! Check the logs above.${NC}"' ERR

# Run main function
main "$@"
