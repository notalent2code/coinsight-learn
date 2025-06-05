#!/bin/bash

echo "🔍 Deploying Coinsight Observability Stack"
echo "==========================================="

# Check if main platform is deployed
echo "🔍 Checking if Coinsight Platform is deployed..."
if ! kubectl get namespace coinsight &> /dev/null; then
    echo "❌ Coinsight namespace not found. Please run ./k8s/scripts/deploy.sh first"
    exit 1
fi

# Check if core services are running
core_services_ready=$(kubectl get pods -n coinsight --no-headers | grep -E "(gateway-service|auth-service)" | grep "Running" | wc -l)
if [ $core_services_ready -lt 2 ]; then
    echo "❌ Core services are not ready. Please ensure main deployment is completed first"
    echo "   Run: ./k8s/scripts/deploy.sh"
    exit 1
fi

echo "✅ Core platform is ready, proceeding with monitoring deployment..."
echo ""

# Set context to coinsight namespace
kubectl config set-context --current --namespace=coinsight

# Add Helm repositories
echo "📦 Adding Helm repositories..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Update dependencies
echo "🔄 Updating Helm dependencies..."
cd k8s/charts/coinsight-platform

# Clean up any existing lock file that might be out of sync
if [ -f "Chart.lock" ]; then
    echo "🧹 Removing existing Chart.lock file..."
    rm -f Chart.lock
fi

# Use dependency update instead of build to ensure fresh dependencies
if ! helm dependency update; then
    echo "❌ Failed to update chart dependencies"
    echo "💡 Make sure all helm repositories are added correctly"
    echo "💡 To clean up: rm -rf charts/*.tgz Chart.lock"
    exit 1
fi
cd ../../..
echo "✅ Chart dependencies updated"
echo ""

# Deploy with monitoring enabled
echo "🚀 Deploying Coinsight Platform with Observability Stack..."
echo "   This will enable monitoring components on the existing deployment..."
helm upgrade --install coinsight-platform k8s/charts/coinsight-platform \
    --namespace coinsight \
    --set microservices.authService.enabled=true \
    --set microservices.transactionService.enabled=true \
    --set microservices.ocrService.enabled=true \
    --set microservices.budgetService.enabled=true \
    --set microservices.notificationService.enabled=true \
    --set microservices.gatewayService.enabled=true \
    --set monitoring.enabled=true \
    --set monitoring.prometheus.enabled=true \
    --set monitoring.grafana.enabled=true \
    --set monitoring.loki.enabled=true \
    --set monitoring.promtail.enabled=true \
    --wait --timeout=600s

echo "⏳ Waiting for monitoring services to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=prometheus --timeout=600s -n coinsight || echo "⚠️  Prometheus may still be starting..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana --timeout=600s -n coinsight || echo "⚠️  Grafana may still be starting..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=loki --timeout=600s -n coinsight || echo "⚠️  Loki may still be starting..."

echo ""
echo "📊 Monitoring stack status:"
kubectl get pods -l "app.kubernetes.io/component in (prometheus,grafana,loki)" -n coinsight

echo ""
echo "✅ Observability Stack deployed successfully!"
echo ""
echo "🔗 Access URLs:"
echo "   📊 Grafana: http://localhost:3000 (admin/admin123)"
echo "   📈 Prometheus: http://localhost:9090"
echo "   📋 Loki: http://localhost:3100"
echo ""
echo "🚀 Setting up port-forwards..."

# Kill existing port-forwards
pkill -f "kubectl port-forward.*grafana" 2>/dev/null || true
pkill -f "kubectl port-forward.*prometheus" 2>/dev/null || true
pkill -f "kubectl port-forward.*loki" 2>/dev/null || true

sleep 2

# Start port-forwards in background
kubectl port-forward svc/coinsight-platform-grafana 3000:80 -n coinsight >/dev/null 2>&1 &
kubectl port-forward svc/coinsight-platform-prometheus-server 9090:80 -n coinsight >/dev/null 2>&1 &
kubectl port-forward svc/coinsight-platform-loki 3100:3100 -n coinsight >/dev/null 2>&1 &

echo "✅ Port-forwards started in background"
echo ""
echo "🎉 Observability stack is ready!"
echo "   📊 Open Grafana: http://localhost:3000"
echo "   📈 Open Prometheus: http://localhost:9090"
echo "   📋 Loki logs: http://localhost:3100"
echo ""
echo "🛑 To stop port-forwards: pkill -f 'kubectl port-forward'"
