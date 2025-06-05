#!/bin/bash

# Database Connection Helper for Coinsight Platform
set -e

echo "🔗 Coinsight Database Connection Helper"
echo "======================================"

# Check if Kind cluster is running
if ! kind get clusters | grep -q "kind"; then
    echo "❌ Kind cluster not found. Please start the cluster first."
    exit 1
fi

# Check if kubectl is configured
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ kubectl is not configured or cluster is not running"
    exit 1
fi

# Check if coinsight namespace exists
if ! kubectl get namespace coinsight &> /dev/null; then
    echo "❌ Coinsight namespace not found. Please deploy the platform first."
    exit 1
fi

echo "✅ Kubernetes cluster is ready"
echo ""

# Function to check database status
check_database() {
    local db_name=$1
    local pod_name="coinsight-platform-postgres-${db_name}"
    
    if kubectl get pods -n coinsight | grep -q "${pod_name}.*Running"; then
        echo "✅ ${db_name} database is running"
        return 0
    else
        echo "❌ ${db_name} database is not ready"
        return 1
    fi
}

# Check all databases
echo "📊 Checking database status..."
databases=("auth" "transaction" "budget" "notification" "keycloak")
all_ready=true

for db in "${databases[@]}"; do
    if ! check_database "$db"; then
        all_ready=false
    fi
done

echo ""

if [ "$all_ready" = true ]; then
    echo "🎉 All databases are ready for connections!"
    echo ""
    echo "📋 DBeaver Connection Details:"
    echo "=============================="
    echo ""
    echo "Auth Service DB       → localhost:5001/auth_service"
    echo "Transaction Service DB → localhost:5002/transaction_service"
    echo "Budget Service DB     → localhost:5003/budget_service"
    echo "Notification Service DB → localhost:5004/notification_service"
    echo "Keycloak DB          → localhost:5005/keycloak"
    echo ""
    echo "Username: postgres"
    echo "Password: postgres"
    echo ""
    echo "💡 Import the connection configurations from dbeaver-connections.md"
else
    echo "⚠️  Some databases are not ready. Please wait and try again."
    echo "💡 Run 'kubectl get pods -n coinsight | grep postgres' to check status"
fi

echo ""
echo "🔧 Useful Commands:"
echo "  View database pods: kubectl get pods -n coinsight | grep postgres"
echo "  View database services: kubectl get svc -n coinsight | grep postgres"
echo "  View database logs: kubectl logs -n coinsight deployment/coinsight-platform-postgres-auth"
echo "  Manual port forward: kubectl port-forward -n coinsight svc/coinsight-platform-postgres-auth 5001:5432"
