#!/bin/bash

# Deploy Coinsight Platform to Kubernetes
set -e

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

# Deploy infrastructure components in order
echo "1️⃣ Installing PostgreSQL instances..."
helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
    --namespace coinsight \
    --set microservices.authService.enabled=false \
    --set microservices.transactionService.enabled=false \
    --set microservices.ocrService.enabled=false \
    --set microservices.budgetService.enabled=false \
    --set microservices.notificationService.enabled=false \
    --set microservices.gatewayService.enabled=false \
    --wait --timeout=300s

echo "⏳ Waiting for databases to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgresql --timeout=300s

echo "3️⃣ Deploying all microservices..."
helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
    --namespace coinsight \
    --wait --timeout=600s

echo "⏳ Waiting for all services to be ready..."
kubectl wait --for=condition=ready pod --all --timeout=600s --ignore-not-found=true

echo ""
echo "🎉 Deployment completed successfully!"
echo ""
echo "📋 Checking service status:"
kubectl get pods -o wide
echo ""
echo "🌐 Service endpoints:"
kubectl get services
echo ""
echo "🔗 Gateway service should be accessible at:"
echo "  NodePort: http://localhost:30080"
echo "  Ingress: http://coinsight.local (add to /etc/hosts: 127.0.0.1 coinsight.local)"
echo ""
echo "🔍 Check logs with:"
echo "  kubectl logs -f deployment/gateway-service"
echo ""
echo "🔍 Check database init job logs:"
echo "  kubectl logs job/auth-db-init -n coinsight"
echo "  kubectl logs job/transaction-db-init -n coinsight"
echo "  kubectl logs job/budget-db-init -n coinsight"
echo "  kubectl logs job/notification-db-init -n coinsight"