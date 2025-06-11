#!/bin/bash

# Coinsight Cloud Deployment Script for Google Cloud Platform (GKE)
# Optimized for Jakarta, Indonesia region

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PROJECT_ID=""  # Will be set interactively
CLUSTER_NAME="coinsight-cluster"
REGION="asia-southeast2"  # Jakarta, Indonesia
ZONE="asia-southeast2-a"
MACHINE_TYPE="e2-standard-2"  # 2 vCPU, 8GB RAM - suitable for microservices
NODE_COUNT=2

echo -e "${BLUE}🚀 Coinsight Cloud Deployment for GCP (Jakarta Region)${NC}"
echo -e "${YELLOW}================================================${NC}"
echo ""
echo -e "${YELLOW}📍 Target Region: ${REGION} (Jakarta, Indonesia)${NC}"
echo -e "${YELLOW}🎯 Cluster Type: GKE Standard (cost-optimized)${NC}"
echo -e "${YELLOW}💰 Estimated Cost: ~$50-80/month (can be reduced with preemptible nodes)${NC}"
echo ""

# Check prerequisites
check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"
    
    # Check if gcloud is installed
    if ! command -v gcloud &> /dev/null; then
        echo -e "${RED}❌ Google Cloud CLI not installed${NC}"
        echo -e "${YELLOW}💡 Install from: https://cloud.google.com/sdk/docs/install${NC}"
        echo -e "${YELLOW}💡 Or use Cloud Shell: https://shell.cloud.google.com${NC}"
        exit 1
    fi
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        echo -e "${YELLOW}⚠️  kubectl not installed. Installing via gcloud...${NC}"
        gcloud components install kubectl
    fi
    
    # Check if Helm is installed
    if ! command -v helm &> /dev/null; then
        echo -e "${YELLOW}⚠️  Helm not installed. Installing...${NC}"
        curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
    fi
    
    echo -e "${GREEN}✅ Prerequisites checked${NC}"
}

# Setup GCP project
setup_gcp_project() {
    echo -e "${BLUE}🔧 Setting up GCP project...${NC}"
    
    # Get project ID interactively if not set
    if [ -z "$PROJECT_ID" ]; then
        echo -e "${YELLOW}📝 Enter your GCP Project ID (create one at https://console.cloud.google.com):${NC}"
        read -p "Project ID: " PROJECT_ID
        
        if [ -z "$PROJECT_ID" ]; then
            echo -e "${RED}❌ Project ID is required${NC}"
            exit 1
        fi
    fi
    
    # Set the project
    gcloud config set project $PROJECT_ID
    
    # Enable required APIs
    echo -e "${BLUE}🔌 Enabling required APIs...${NC}"
    gcloud services enable container.googleapis.com \
        compute.googleapis.com \
        artifactregistry.googleapis.com \
        cloudbuild.googleapis.com
    
    echo -e "${GREEN}✅ GCP project configured: $PROJECT_ID${NC}"
}

# Create Artifact Registry for Docker images
setup_artifact_registry() {
    echo -e "${BLUE}📦 Setting up Artifact Registry...${NC}"
    
    REGISTRY_NAME="coinsight-registry"
    
    # Create registry if it doesn't exist
    if ! gcloud artifacts repositories describe $REGISTRY_NAME --location=$REGION &> /dev/null; then
        gcloud artifacts repositories create $REGISTRY_NAME \
            --repository-format=docker \
            --location=$REGION \
            --description="Coinsight microservices container registry"
    fi
    
    # Configure Docker authentication
    gcloud auth configure-docker ${REGION}-docker.pkg.dev
    
    echo -e "${GREEN}✅ Artifact Registry configured: ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REGISTRY_NAME}${NC}"
}

# Create GKE cluster
create_gke_cluster() {
    echo -e "${BLUE}☸️  Creating GKE cluster...${NC}"
    
    # Check if cluster already exists
    if gcloud container clusters describe $CLUSTER_NAME --zone=$ZONE &> /dev/null; then
        echo -e "${YELLOW}⚠️  Cluster $CLUSTER_NAME already exists${NC}"
        read -p "Do you want to delete and recreate it? (y/N): " recreate
        if [[ $recreate =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}🗑️  Deleting existing cluster...${NC}"
            gcloud container clusters delete $CLUSTER_NAME --zone=$ZONE --quiet
        else
            echo -e "${BLUE}📍 Using existing cluster${NC}"
            gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE
            return
        fi
    fi
    
    echo -e "${BLUE}🏗️  Creating new GKE cluster (this may take 5-10 minutes)...${NC}"
    gcloud container clusters create $CLUSTER_NAME \
        --zone=$ZONE \
        --machine-type=$MACHINE_TYPE \
        --num-nodes=$NODE_COUNT \
        --enable-autoscaling \
        --min-nodes=1 \
        --max-nodes=4 \
        --enable-autorepair \
        --enable-autoupgrade \
        --disk-size=50GB \
        --disk-type=pd-standard \
        --image-type=COS_CONTAINERD \
        --enable-ip-alias \
        --network=default \
        --subnetwork=default \
        --preemptible  # Cost optimization: ~70% cheaper
    
    # Get cluster credentials
    gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE
    
    echo -e "${GREEN}✅ GKE cluster created and configured${NC}"
}

# Build and push Docker images to Artifact Registry
build_and_push_images() {
    echo -e "${BLUE}🔨 Building and pushing Docker images...${NC}"
    
    REGISTRY_URL="${REGION}-docker.pkg.dev/${PROJECT_ID}/coinsight-registry"
    
    # Services to build
    SERVICES=("auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")
    
    for service in "${SERVICES[@]}"; do
        echo -e "${YELLOW}📦 Building $service...${NC}"
        
        # Create a simple cloudbuild.yaml for this service
        cat > cloudbuild-${service}.yaml << EOF
steps:
- name: 'gcr.io/cloud-builders/docker'
  args: ['build', '-f', '${service}/Dockerfile.cloudbuild', '-t', '${REGISTRY_URL}/${service}:latest', '.']
- name: 'gcr.io/cloud-builders/docker'
  args: ['push', '${REGISTRY_URL}/${service}:latest']
timeout: 1200s
EOF
        
        # Build with Cloud Build using config file
        gcloud builds submit \
            --config=cloudbuild-${service}.yaml \
            --timeout=20m \
            .
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ ${service} built and pushed successfully${NC}"
            rm -f cloudbuild-${service}.yaml
        else
            echo -e "${RED}❌ Failed to build ${service}${NC}"
            rm -f cloudbuild-${service}.yaml
            exit 1
        fi
    done
    
    echo -e "${GREEN}🎉 All images built and pushed to Artifact Registry${NC}"
}

# Deploy to GKE using modified Helm charts
deploy_to_gke() {
    echo -e "${BLUE}🚀 Deploying Coinsight platform to GKE...${NC}"
    
    # Create namespace
    kubectl create namespace coinsight --dry-run=client -o yaml | kubectl apply -f -
    
    # Add Helm repositories
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo update
    
    # Create GKE-specific values file
    create_gke_values_file
    
    # Deploy infrastructure first
    echo -e "${BLUE}1️⃣ Deploying infrastructure...${NC}"
    helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
        --namespace coinsight \
        --values cloud-deploy/gke-values.yaml \
        --set microservices.authService.enabled=false \
        --set microservices.transactionService.enabled=false \
        --set microservices.ocrService.enabled=false \
        --set microservices.budgetService.enabled=false \
        --set microservices.notificationService.enabled=false \
        --set microservices.gatewayService.enabled=false \
        --set monitoring.enabled=false \
        --wait --timeout=600s
    
    echo -e "${BLUE}⏳ Waiting for infrastructure to be ready...${NC}"
    kubectl wait --for=condition=ready pod -l "app.kubernetes.io/name" --timeout=300s -n coinsight
    
    # Initialize Keycloak (modified for GKE)
    initialize_keycloak_gke
    
    # Deploy microservices
    echo -e "${BLUE}2️⃣ Deploying microservices...${NC}"
    helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
        --namespace coinsight \
        --values cloud-deploy/gke-values.yaml \
        --set microservices.authService.enabled=true \
        --set microservices.transactionService.enabled=true \
        --set microservices.ocrService.enabled=true \
        --set microservices.budgetService.enabled=true \
        --set microservices.notificationService.enabled=true \
        --set microservices.gatewayService.enabled=true \
        --set monitoring.enabled=false \
        --wait --timeout=600s
    
    echo -e "${GREEN}✅ Deployment completed!${NC}"
}

# Create GKE-specific values file
create_gke_values_file() {
    echo -e "${BLUE}📝 Creating GKE-specific values file...${NC}"
    
    REGISTRY_URL="${REGION}-docker.pkg.dev/${PROJECT_ID}/coinsight-registry"
    
    cat > cloud-deploy/gke-values.yaml << EOF
# GKE-specific values for Coinsight Platform
global:
  namespace: "coinsight"
  image:
    registry: "${REGISTRY_URL}"
    pullPolicy: Always
    tag: "latest"

# Use LoadBalancer instead of NodePort for GKE
microservices:
  authService:
    image:
      repository: "${REGISTRY_URL}/auth-service"
  transactionService:
    image:
      repository: "${REGISTRY_URL}/transaction-service"
  ocrService:
    image:
      repository: "${REGISTRY_URL}/ocr-service"
  budgetService:
    image:
      repository: "${REGISTRY_URL}/budget-service"
  notificationService:
    image:
      repository: "${REGISTRY_URL}/notification-service"
  gatewayService:
    image:
      repository: "${REGISTRY_URL}/gateway-service"
    service:
      type: LoadBalancer  # GKE will provide external IP
      port: 8080

# Use persistent disks for databases
postgres-auth:
  primary:
    persistence:
      storageClass: "standard-rwo"
      size: 10Gi

postgres-transaction:
  primary:
    persistence:
      storageClass: "standard-rwo"
      size: 20Gi

postgres-budget:
  primary:
    persistence:
      storageClass: "standard-rwo"
      size: 10Gi

postgres-notification:
  primary:
    persistence:
      storageClass: "standard-rwo"
      size: 10Gi

postgres-keycloak:
  primary:
    persistence:
      storageClass: "standard-rwo"
      size: 10Gi

kafka:
  broker:
    persistence:
      storageClass: "standard-rwo"
      size: 20Gi
  zookeeper:
    persistence:
      storageClass: "standard-rwo"
      size: 10Gi

redis:
  master:
    persistence:
      storageClass: "standard-rwo"
      size: 8Gi

keycloak:
  service:
    type: LoadBalancer  # External access for admin
EOF

    echo -e "${GREEN}✅ GKE values file created${NC}"
}

# Initialize Keycloak for GKE
initialize_keycloak_gke() {
    echo -e "${BLUE}🔑 Initializing Keycloak for GKE...${NC}"
    
    # Wait for Keycloak to be ready
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak --timeout=300s -n coinsight
    
    # Get Keycloak external IP
    echo -e "${YELLOW}⏳ Waiting for Keycloak LoadBalancer IP...${NC}"
    KEYCLOAK_IP=""
    for i in {1..30}; do
        KEYCLOAK_IP=$(kubectl get service coinsight-platform-keycloak -n coinsight -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
        if [ -n "$KEYCLOAK_IP" ]; then
            break
        fi
        echo "   Waiting for external IP... ($i/30)"
        sleep 10
    done
    
    if [ -z "$KEYCLOAK_IP" ]; then
        echo -e "${RED}❌ Failed to get Keycloak external IP${NC}"
        echo -e "${YELLOW}💡 You may need to configure Keycloak manually${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Keycloak available at: http://${KEYCLOAK_IP}${NC}"
    
    # Modify and run Keycloak initialization
    # Create a modified version of the init script for GKE
    sed "s|http://localhost:8090|http://${KEYCLOAK_IP}|g" k8s/scripts/init-keycloak-realm.sh > cloud-deploy/init-keycloak-gke.sh
    chmod +x cloud-deploy/init-keycloak-gke.sh
    
    # Run initialization
    if ./cloud-deploy/init-keycloak-gke.sh; then
        echo -e "${GREEN}✅ Keycloak initialized successfully${NC}"
    else
        echo -e "${YELLOW}⚠️  Keycloak initialization may need manual completion${NC}"
    fi
}

# Get access information
get_access_info() {
    echo -e "${BLUE}🌐 Getting access information...${NC}"
    
    # Get Gateway external IP
    GATEWAY_IP=$(kubectl get service gateway-service -n coinsight -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
    KEYCLOAK_IP=$(kubectl get service coinsight-platform-keycloak -n coinsight -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
    
    echo ""
    echo -e "${GREEN}🎉 Deployment Completed Successfully!${NC}"
    echo -e "${YELLOW}===========================================${NC}"
    echo ""
    echo -e "${BLUE}🔗 Access URLs:${NC}"
    if [ -n "$GATEWAY_IP" ]; then
        echo -e "   🚪 Gateway (Main API): http://${GATEWAY_IP}:8080"
        echo -e "   🔍 Health Check: http://${GATEWAY_IP}:8080/actuator/health"
    else
        echo -e "   ⏳ Gateway IP pending... Check with: kubectl get service gateway-service -n coinsight"
    fi
    
    if [ -n "$KEYCLOAK_IP" ]; then
        echo -e "   🔐 Keycloak Admin: http://${KEYCLOAK_IP} (admin/admin)"
    else
        echo -e "   ⏳ Keycloak IP pending... Check with: kubectl get service coinsight-platform-keycloak -n coinsight"
    fi
    echo ""
    echo -e "${BLUE}📊 Monitoring:${NC}"
    echo -e "   💰 Cost Monitoring: https://console.cloud.google.com/billing"
    echo -e "   📈 GKE Dashboard: https://console.cloud.google.com/kubernetes"
    echo -e "   📋 Logs: https://console.cloud.google.com/logs"
    echo ""
    echo -e "${BLUE}🛠️  Management Commands:${NC}"
    echo -e "   📱 Connect to cluster: gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE"
    echo -e "   🔍 Check pods: kubectl get pods -n coinsight"
    echo -e "   📊 Check services: kubectl get services -n coinsight"
    echo -e "   🗑️  Delete cluster: gcloud container clusters delete $CLUSTER_NAME --zone=$ZONE"
    echo ""
    echo -e "${YELLOW}💡 Total setup time: ~20-30 minutes${NC}"
    echo -e "${YELLOW}💰 Estimated monthly cost: $50-80 USD (with preemptible nodes)${NC}"
}

# Cleanup function
cleanup_on_error() {
    echo -e "${RED}❌ Error occurred. Cleaning up...${NC}"
    # Add cleanup logic here if needed
}

# Main execution
main() {
    # Set up error handling
    trap cleanup_on_error ERR
    
    # Create cloud-deploy directory
    mkdir -p cloud-deploy
    
    # Run deployment steps
    check_prerequisites
    setup_gcp_project
    setup_artifact_registry
    create_gke_cluster
    build_and_push_images
    deploy_to_gke
    get_access_info
    
    echo -e "${GREEN}🎉 Cloud deployment completed successfully!${NC}"
}

# Run main function
main "$@"
