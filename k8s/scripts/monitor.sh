#!/bin/bash

# Monitor Coinsight Platform deployment status
echo "🔍 Monitoring Coinsight Platform Deployment Status"
echo "=================================================="

# Function to get pod status with colors
get_pod_status() {
    kubectl get pods -n coinsight -o custom-columns=NAME:.metadata.name,STATUS:.status.phase,READY:.status.containerStatuses[*].ready,RESTARTS:.status.containerStatuses[*].restartCount 2>/dev/null
}

# Function to get service status
get_service_status() {
    kubectl get services -n coinsight 2>/dev/null
}

# Function to check readiness
check_readiness() {
    echo ""
    echo "🔍 Service Readiness Check:"
    echo "-------------------------"
    
    # Check config-server
    if kubectl get pod -l app=config-server -n coinsight --no-headers 2>/dev/null | grep -q "Running"; then
        echo "✅ Config Server: Running"
    else
        echo "🔄 Config Server: Not Ready"
    fi
    
    # Check databases
    if kubectl get pod -l app.kubernetes.io/name=postgresql -n coinsight --no-headers 2>/dev/null | grep -q "Running"; then
        echo "✅ PostgreSQL: Running"
    else
        echo "🔄 PostgreSQL: Not Ready"
    fi
    
    # Check other services
    services=("auth-service" "transaction-service" "ocr-service" "budget-service" "notification-service" "gateway-service")
    for service in "${services[@]}"; do
        if kubectl get pod -l app=$service -n coinsight --no-headers 2>/dev/null | grep -q "Running"; then
            echo "✅ $service: Running"
        else
            echo "🔄 $service: Not Ready"
        fi
    done
}

# Main monitoring loop
while true; do
    clear
    echo "🔍 Monitoring Coinsight Platform Deployment Status"
    echo "=================================================="
    echo "⏰ $(date)"
    echo ""
    
    echo "📦 Pod Status:"
    echo "--------------"
    get_pod_status
    
    echo ""
    echo "🌐 Services:"
    echo "------------"
    get_service_status
    
    check_readiness
    
    echo ""
    echo "🔄 Press Ctrl+C to stop monitoring"
    echo "🔍 Refreshing in 10 seconds..."
    
    sleep 10
done
