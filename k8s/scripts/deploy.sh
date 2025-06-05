#!/bin/bash

# Deploy Coinsight Platform to Kubernetes
set -e

# Function to show deployment progress
show_progress() {
    local message=$1
    local timeout=${2:-60}
    local interval=${3:-10}
    local iterations=$((timeout / interval))
    
    echo "$message"
    for ((i=1; i<=iterations; i++)); do
        echo "   ⏱️  Progress check ${i}/${iterations}..."
        kubectl get pods -o wide | grep -E "(Running|Pending|ContainerCreating|ImagePullBackOff|CrashLoopBackOff)" | head -10
        if [ $i -lt $iterations ]; then
            echo "   ⏳ Waiting ${interval}s before next check..."
            sleep $interval
        fi
    done
    echo ""
}

echo "🚀 Deploying Coinsight Platform to Kubernetes..."

# Check if kubectl is configured
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ kubectl is not configured or cluster is not running"
    exit 1
fi

# Check if namespace exists
if ! kubectl get namespace coinsight &> /dev/null; then
    echo "📁 Creating coinsight namespace..."
    kubectl create namespace coinsight
else
    echo "✅ Namespace 'coinsight' already exists"
fi

# Set context to coinsight namespace
kubectl config set-context --current --namespace=coinsight

echo "🧹 Cleaning up any existing database init jobs..."
kubectl delete jobs -l "helm.sh/hook" -n coinsight --ignore-not-found=true

echo "📦 Deploying infrastructure dependencies first..."
echo "   This includes PostgreSQL databases, Redis, Kafka, and Keycloak"
echo ""

# Deploy infrastructure components in order
echo "1️⃣ Installing PostgreSQL instances and infrastructure components..."
echo "   ➤ Deploying: PostgreSQL (auth, transaction, budget, notification, keycloak)"
echo "   ➤ Deploying: Redis cluster"
echo "   ➤ Deploying: Kafka with KRaft"
echo "   ➤ Deploying: Keycloak identity server"
echo "   ➤ Microservices are DISABLED in this step"
echo ""
echo "🚀 Running Helm upgrade for infrastructure..."
echo "   Command: helm upgrade --install coinsight-platform k8s/charts/coinsight-platform"
echo "   Parameters: microservices disabled, wait enabled, timeout 300s"
echo ""

helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
    --namespace coinsight \
    --set microservices.authService.enabled=false \
    --set microservices.transactionService.enabled=false \
    --set microservices.ocrService.enabled=false \
    --set microservices.budgetService.enabled=false \
    --set microservices.notificationService.enabled=false \
    --set microservices.gatewayService.enabled=false \
    --wait --timeout=300s

echo "✅ Infrastructure deployment completed!"
echo ""
echo "📊 Current pod status after infrastructure deployment:"
kubectl get pods -o wide
echo ""

echo "⏳ Waiting for PostgreSQL databases to be ready..."
echo "   This may take 1-2 minutes for all databases to start up..."

# Show progress while waiting for databases
echo "🔄 Monitoring database startup..."
for i in {1..12}; do
    echo "   ⏱️  Database check ${i}/12 (every 15s)..."
    kubectl get pods | grep postgres | grep -v NAME
    ready_count=$(kubectl get pods --no-headers | grep postgres | grep "1/1.*Running" | wc -l)
    total_count=$(kubectl get pods --no-headers | grep postgres | wc -l)
    echo "   📊 Ready: ${ready_count}/${total_count} PostgreSQL databases"
    
    if [ "$ready_count" -eq "$total_count" ] && [ "$total_count" -gt 0 ]; then
        echo "   ✅ All databases are ready!"
        break
    fi
    
    if [ $i -lt 12 ]; then
        echo "   ⏳ Waiting 15s before next check..."
        sleep 15
    fi
done

kubectl wait --for=condition=ready pod -l "app.kubernetes.io/name" --timeout=60s | grep postgres || true

echo "✅ All PostgreSQL databases are ready!"
echo ""
echo "📊 Database pod status:"
kubectl get pods | grep postgres
echo ""

echo "🔍 Checking infrastructure health before deploying microservices..."
echo "   ➤ Kafka status:"
kubectl get pods -l app.kubernetes.io/name=kafka
echo "   ➤ Redis status:"
kubectl get pods -l app.kubernetes.io/name=redis
echo "   ➤ Keycloak status:"
kubectl get pods -l app.kubernetes.io/name=keycloak
echo ""

# Initialize Keycloak realm after infrastructure is ready
initialize_keycloak_realm() {
    echo "🔑 Initializing Keycloak realm and clients..."
    echo "   This step configures OAuth2 clients for all microservices"
    echo ""
    
    # Check if Keycloak pod is ready
    echo "⏳ Waiting for Keycloak pod to be ready..."
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak --timeout=120s
    
    # Run the Keycloak initialization script (it will handle connectivity checks)
    echo "🚀 Executing Keycloak realm initialization..."
    if [ -f "k8s/scripts/init-keycloak-realm.sh" ]; then
        chmod +x k8s/scripts/init-keycloak-realm.sh
        
        # Run with error handling
        if DEBUG_SHOW_SECRETS=false ./k8s/scripts/init-keycloak-realm.sh; then
            echo "✅ Keycloak realm initialization completed successfully!"
        else
            echo "❌ Keycloak realm initialization failed"
            return 1
        fi
    else
        echo "❌ Keycloak initialization script not found at k8s/scripts/init-keycloak-realm.sh"
        return 1
    fi
    
    echo "✅ Keycloak realm initialization completed!"
    echo ""
}

# Call the initialization function
initialize_keycloak_realm

echo "2️⃣ Deploying all microservices..."
echo "   ➤ Enabling: auth-service (port 8081)"
echo "   ➤ Enabling: transaction-service (port 8082)"
echo "   ➤ Enabling: ocr-service (port 8083)"
echo "   ➤ Enabling: budget-service (port 8084)"
echo "   ➤ Enabling: notification-service (port 8085)"
echo "   ➤ Enabling: gateway-service (port 8080)"
echo ""
echo "🚀 Running Helm upgrade for microservices..."
echo "   Command: helm upgrade --install coinsight-platform k8s/charts/coinsight-platform"
echo "   Parameters: all microservices enabled, wait enabled, timeout 300s"
echo ""

helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
    --namespace coinsight \
    --set microservices.authService.enabled=true \
    --set microservices.transactionService.enabled=true \
    --set microservices.ocrService.enabled=true \
    --set microservices.budgetService.enabled=true \
    --set microservices.notificationService.enabled=true \
    --set microservices.gatewayService.enabled=true \
    --wait --timeout=300s

echo "✅ Microservices deployment completed!"
echo ""
echo "📊 Current pod status after microservices deployment:"
kubectl get pods -o wide
echo ""

echo "⏳ Waiting for all services to be ready..."
echo "   This may take 2-3 minutes for all microservices to start up..."
echo "   Services need to:"
echo "   ➤ Download/load Docker images"
echo "   ➤ Initialize Spring Boot applications"
echo "   ➤ Connect to databases and Keycloak"
echo "   ➤ Pass health checks"
echo ""

# Show progress while waiting
echo "🔄 Monitoring startup progress..."
for i in {1..3}; do
    echo "   ⏱️  Checking startup progress (${i}/3)..."
    kubectl get pods --no-headers | grep -E "(auth-service|transaction-service|ocr-service|budget-service|notification-service|gateway-service)" | while read line; do
        name=$(echo $line | awk '{print $1}')
        status=$(echo $line | awk '{print $3}')
        ready=$(echo $line | awk '{print $2}')
        echo "      ➤ $name: $status ($ready)"
    done
    echo ""
    sleep 20
done

kubectl wait --for=condition=ready pod --all --timeout=300s

echo "🔄 Restarting deployments to ensure latest image is used..."
kubectl rollout restart deployment -n coinsight

echo ""
echo "🎉 Deployment completed successfully!"
echo ""
echo "📋 Final service status:"
kubectl get pods -o wide
echo ""
echo "🌐 Service endpoints:"
kubectl get services
echo ""
echo "🔗 Gateway service should be accessible at:"
echo "  NodePort: http://localhost:30080"
echo "  Ingress: http://coinsight.local (add to /etc/hosts: 127.0.0.1 coinsight.local)"
echo ""
echo "🔍 Useful commands:"
echo "  Check gateway logs:           kubectl logs -f deployment/gateway-service"
echo "  Check auth service logs:      kubectl logs -f deployment/auth-service"
echo "  Check transaction logs:       kubectl logs -f deployment/transaction-service"
echo "  Check all service status:     kubectl get pods -w"
echo ""
echo "🔍 Check database init job logs:"
echo "  kubectl logs job/auth-db-init -n coinsight"
echo "  kubectl logs job/transaction-db-init -n coinsight"
echo "  kubectl logs job/budget-db-init -n coinsight"
echo "  kubectl logs job/notification-db-init -n coinsight"
echo ""
echo "🎯 Next steps:"
echo "  1. Test the gateway: curl http://localhost:30080/actuator/health"
echo "  2. Monitor services: kubectl get pods -w"
echo "  3. Check Keycloak admin console: http://localhost:8090 (admin/admin)"
echo ""
echo "📧 MailHog (for testing emails): http://localhost:31025"