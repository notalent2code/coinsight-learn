#!/bin/bash

# Coinsight VPS Deployment Script
# For budget-friendly deployment on any VPS provider
# Tested on: DigitalOcean, Vultr, Linode, Hetzner

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Coinsight VPS Deployment Script${NC}"
echo -e "${YELLOW}=================================${NC}"
echo ""
echo -e "${YELLOW}💰 Budget-Friendly VPS Providers (Jakarta region):${NC}"
echo -e "   🇮🇩 Vultr Singapore: $5-10/month (512MB-2GB RAM)"
echo -e "   🇮🇩 DigitalOcean Singapore: $6-12/month (1-2GB RAM)"
echo -e "   🇮🇩 Linode Singapore: $5-10/month (1-2GB RAM)"
echo -e "   🇮🇩 Hetzner Finland: $4-8/month (2-4GB RAM) - Best value!"
echo ""

# Check if running on VPS
check_vps_environment() {
    echo -e "${BLUE}🔍 Checking VPS environment...${NC}"
    
    # Check available RAM
    TOTAL_RAM=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    echo -e "${YELLOW}💾 Available RAM: ${TOTAL_RAM}MB${NC}"
    
    if [ "$TOTAL_RAM" -lt 1500 ]; then
        echo -e "${RED}⚠️  Warning: Less than 1.5GB RAM detected${NC}"
        echo -e "${YELLOW}💡 Recommended: At least 2GB RAM for comfortable operation${NC}"
        read -p "Continue anyway? (y/N): " continue_anyway
        if [[ ! $continue_anyway =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # Check disk space
    DISK_AVAILABLE=$(df / | awk 'NR==2{printf "%.0f", $4/1024}')
    echo -e "${YELLOW}💿 Available Disk: ${DISK_AVAILABLE}MB${NC}"
    
    if [ "$DISK_AVAILABLE" -lt 10000 ]; then
        echo -e "${RED}⚠️  Warning: Less than 10GB disk space available${NC}"
        echo -e "${YELLOW}💡 Recommended: At least 20GB disk space${NC}"
    fi
    
    echo -e "${GREEN}✅ Environment check completed${NC}"
}

# Install Docker and Docker Compose
install_docker() {
    echo -e "${BLUE}🐳 Installing Docker...${NC}"
    
    # Update system
    sudo apt-get update
    
    # Install Docker if not present
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}📦 Installing Docker...${NC}"
        curl -fsSL https://get.docker.com -o get-docker.sh
        sudo sh get-docker.sh
        sudo usermod -aG docker $USER
        rm get-docker.sh
    else
        echo -e "${GREEN}✅ Docker already installed${NC}"
    fi
    
    # Install Docker Compose if not present
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${YELLOW}📦 Installing Docker Compose...${NC}"
        sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    else
        echo -e "${GREEN}✅ Docker Compose already installed${NC}"
    fi
    
    echo -e "${GREEN}✅ Docker installation completed${NC}"
}

# Install K3s (lightweight Kubernetes)
install_k3s() {
    echo -e "${BLUE}☸️  Installing K3s (lightweight Kubernetes)...${NC}"
    
    if command -v k3s &> /dev/null; then
        echo -e "${GREEN}✅ K3s already installed${NC}"
        return
    fi
    
    # Install K3s with minimal resource usage
    curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik --disable metrics-server" sh -
    
    # Set up kubectl for current user
    sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config 2>/dev/null || mkdir -p ~/.kube && sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
    sudo chown $(id -u):$(id -g) ~/.kube/config
    
    # Add kubectl alias for k3s
    echo 'alias kubectl="k3s kubectl"' >> ~/.bashrc
    source ~/.bashrc
    
    echo -e "${GREEN}✅ K3s installed successfully${NC}"
}

# Install Helm
install_helm() {
    echo -e "${BLUE}⚓ Installing Helm...${NC}"
    
    if command -v helm &> /dev/null; then
        echo -e "${GREEN}✅ Helm already installed${NC}"
        return
    fi
    
    curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
    
    echo -e "${GREEN}✅ Helm installed successfully${NC}"
}

# Build Docker images locally
build_images_locally() {
    echo -e "${BLUE}🔨 Building Docker images locally...${NC}"
    
    SERVICES=("auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")
    
    for service in "${SERVICES[@]}"; do
        echo -e "${YELLOW}📦 Building $service...${NC}"
        
        docker build \
            -f "$service/Dockerfile" \
            -t "coinsight/${service}:latest" \
            . || {
            echo -e "${RED}❌ Failed to build $service${NC}"
            exit 1
        }
        
        echo -e "${GREEN}✅ $service built successfully${NC}"
    done
    
    echo -e "${GREEN}🎉 All images built successfully${NC}"
}

# Create VPS-optimized values file
create_vps_values() {
    echo -e "${BLUE}📝 Creating VPS-optimized values...${NC}"
    
    cat > vps-values.yaml << 'EOF'
# VPS-optimized values for Coinsight Platform
global:
  namespace: "coinsight"
  image:
    registry: "coinsight"
    pullPolicy: IfNotPresent
    tag: "latest"
  
  # Reduced resource requirements for VPS
  resources:
    limits:
      memory: "512Mi"
      cpu: "500m"

# Microservices with reduced resources
microservices:
  authService:
    enabled: true
    image:
      repository: "coinsight/auth-service"
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"

  transactionService:
    enabled: true
    image:
      repository: "coinsight/transaction-service"
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"

  ocrService:
    enabled: true
    image:
      repository: "coinsight/ocr-service"
    resources:
      requests:
        memory: "512Mi"
        cpu: "200m"
      limits:
        memory: "1Gi"
        cpu: "500m"

  budgetService:
    enabled: true
    image:
      repository: "coinsight/budget-service"
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"

  notificationService:
    enabled: true
    image:
      repository: "coinsight/notification-service"
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"

  gatewayService:
    enabled: true
    image:
      repository: "coinsight/gateway-service"
    service:
      type: NodePort
      nodePort: 30080
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"

# Reduced database resources
postgres-auth:
  enabled: true
  auth:
    database: "auth_service"
    username: "postgres"
    password: "postgres"
  primary:
    persistence:
      size: 2Gi
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"

postgres-transaction:
  enabled: true
  auth:
    database: "transaction_service"
    username: "postgres"
    password: "postgres"
  primary:
    persistence:
      size: 3Gi
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"

postgres-budget:
  enabled: true
  auth:
    database: "budget_service"
    username: "postgres"
    password: "postgres"
  primary:
    persistence:
      size: 2Gi
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"

postgres-notification:
  enabled: true
  auth:
    database: "notification_service"
    username: "postgres"
    password: "postgres"
  primary:
    persistence:
      size: 2Gi
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"

postgres-keycloak:
  enabled: true
  auth:
    database: "keycloak"
    username: "postgres"
    password: "postgres"
  primary:
    persistence:
      size: 2Gi
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"

# Lightweight Kafka setup
kafka:
  enabled: true
  kraft:
    enabled: false
  controller:
    replicaCount: 0
  zookeeper:
    enabled: true
    replicaCount: 1
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "200m"
    persistence:
      size: 2Gi
  broker:
    replicaCount: 1
    resources:
      requests:
        memory: "256Mi"
        cpu: "100m"
      limits:
        memory: "512Mi"
        cpu: "300m"
    persistence:
      size: 4Gi

# Lightweight Redis
redis:
  enabled: true
  auth:
    enabled: false
  master:
    resources:
      requests:
        memory: "64Mi"
        cpu: "25m"
      limits:
        memory: "128Mi"
        cpu: "100m"
    persistence:
      size: 1Gi

# Lightweight Keycloak
keycloak:
  enabled: true
  auth:
    adminUser: "admin"
    adminPassword: "admin"
  service:
    type: NodePort
    nodePorts:
      http: 30090
  postgresql:
    enabled: false
  externalDatabase:
    host: "coinsight-platform-postgres-keycloak"
    port: 5432
    user: "postgres"
    password: "postgres"
    database: "keycloak"
  resources:
    requests:
      memory: "256Mi"
      cpu: "100m"
    limits:
      memory: "512Mi"
      cpu: "300m"

# Disable monitoring for VPS (save resources)
monitoring:
  enabled: false

# Disable mailhog for VPS (save resources)
mailhog:
  enabled: false
EOF

    echo -e "${GREEN}✅ VPS values file created${NC}"
}

# Deploy to K3s
deploy_to_k3s() {
    echo -e "${BLUE}🚀 Deploying to K3s...${NC}"
    
    # Add Helm repositories
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update
    
    # Create namespace
    k3s kubectl create namespace coinsight --dry-run=client -o yaml | k3s kubectl apply -f -
    
    # Deploy infrastructure first
    echo -e "${BLUE}1️⃣ Deploying infrastructure...${NC}"
    helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
        --namespace coinsight \
        --values vps-values.yaml \
        --set microservices.authService.enabled=false \
        --set microservices.transactionService.enabled=false \
        --set microservices.ocrService.enabled=false \
        --set microservices.budgetService.enabled=false \
        --set microservices.notificationService.enabled=false \
        --set microservices.gatewayService.enabled=false \
        --wait --timeout=600s
    
    echo -e "${BLUE}⏳ Waiting for infrastructure...${NC}"
    sleep 60
    
    # Deploy microservices
    echo -e "${BLUE}2️⃣ Deploying microservices...${NC}"
    helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
        --namespace coinsight \
        --values vps-values.yaml \
        --wait --timeout=600s
    
    echo -e "${GREEN}✅ Deployment completed!${NC}"
}

# Setup firewall and security
setup_security() {
    echo -e "${BLUE}🔐 Setting up security...${NC}"
    
    # Enable UFW firewall
    sudo ufw --force enable
    
    # Allow SSH
    sudo ufw allow ssh
    
    # Allow HTTP and HTTPS
    sudo ufw allow 80
    sudo ufw allow 443
    
    # Allow specific ports for Coinsight
    sudo ufw allow 30080  # Gateway
    sudo ufw allow 30090  # Keycloak
    
    # Allow K3s internal communication
    sudo ufw allow 6443
    sudo ufw allow 10250
    
    echo -e "${GREEN}✅ Security configured${NC}"
}

# Get access information
get_vps_access_info() {
    echo -e "${BLUE}🌐 Getting access information...${NC}"
    
    # Get VPS public IP
    PUBLIC_IP=$(curl -s ifconfig.me || curl -s icanhazip.com || curl -s ipinfo.io/ip)
    
    echo ""
    echo -e "${GREEN}🎉 VPS Deployment Completed!${NC}"
    echo -e "${YELLOW}=========================${NC}"
    echo ""
    echo -e "${BLUE}🔗 Access URLs:${NC}"
    echo -e "   🚪 Gateway API: http://${PUBLIC_IP}:30080"
    echo -e "   🔍 Health Check: http://${PUBLIC_IP}:30080/actuator/health"
    echo -e "   🔐 Keycloak Admin: http://${PUBLIC_IP}:30090 (admin/admin)"
    echo ""
    echo -e "${BLUE}🛠️  Management Commands:${NC}"
    echo -e "   📱 Check pods: k3s kubectl get pods -n coinsight"
    echo -e "   📊 Check services: k3s kubectl get services -n coinsight"
    echo -e "   📋 Check logs: k3s kubectl logs -f deployment/gateway-service -n coinsight"
    echo ""
    echo -e "${YELLOW}💡 VPS Management:${NC}"
    echo -e "   🔄 Restart K3s: sudo systemctl restart k3s"
    echo -e "   🛑 Stop services: k3s kubectl scale deployment --replicas=0 --all -n coinsight"
    echo -e "   ▶️  Start services: k3s kubectl scale deployment --replicas=1 --all -n coinsight"
    echo ""
    echo -e "${YELLOW}💰 Cost: $5-10/month (depending on VPS provider)${NC}"
}

# Main execution
main() {
    echo -e "${BLUE}🎯 Starting VPS deployment process...${NC}"
    echo ""
    
    # Check if we should continue
    read -p "Continue with VPS deployment? (y/N): " confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Deployment cancelled${NC}"
        exit 0
    fi
    
    # Run deployment steps
    check_vps_environment
    install_docker
    install_k3s
    install_helm
    build_images_locally
    create_vps_values
    deploy_to_k3s
    setup_security
    get_vps_access_info
    
    echo -e "${GREEN}🎉 VPS deployment completed successfully!${NC}"
}

# Run main function
main "$@"
