# DBeaver Connection Configurations for Coinsight Databases

This file contains the connection details for all Coinsight PostgreSQL databases running in Kubernetes.

## Prerequisites

1. Ensure Kind cluster is running with the updated `kind-config.yaml`
2. Deploy the platform using `k8s/scripts/deploy.sh`
3. Wait for all PostgreSQL pods to be ready

## Database Connections

### 1. Auth Service Database
- **Connection Name**: Coinsight Auth DB
- **Host**: localhost
- **Port**: 5001
- **Database**: auth_service
- **Username**: postgres
- **Password**: postgres
- **Description**: User authentication and authorization data

### 2. Transaction Service Database
- **Connection Name**: Coinsight Transaction DB
- **Host**: localhost
- **Port**: 5002
- **Database**: transaction_service
- **Username**: postgres
- **Password**: postgres
- **Description**: Financial transactions and categories

### 3. Budget Service Database
- **Connection Name**: Coinsight Budget DB
- **Host**: localhost
- **Port**: 5003
- **Database**: budget_service
- **Username**: postgres
- **Password**: postgres
- **Description**: Budget management and alerts

### 4. Notification Service Database
- **Connection Name**: Coinsight Notification DB
- **Host**: localhost
- **Port**: 5004
- **Database**: notification_service
- **Username**: postgres
- **Password**: postgres
- **Description**: Notifications and email templates

### 5. Keycloak Database
- **Connection Name**: Coinsight Keycloak DB
- **Host**: localhost
- **Port**: 5005
- **Database**: keycloak
- **Username**: postgres
- **Password**: postgres
- **Description**: Keycloak identity provider data

## DBeaver Setup Instructions

1. Open DBeaver
2. Click "New Database Connection" (+ icon)
3. Select "PostgreSQL"
4. Enter the connection details for each database
5. Test the connection
6. Save and connect

## Troubleshooting

### Connection Refused
- Ensure Kind cluster is running: `kind get clusters`
- Check if databases are ready: `kubectl get pods -n coinsight | grep postgres`
- Verify port forwarding: `kubectl get svc -n coinsight | grep postgres`

### Authentication Failed
- Double-check username/password (both should be "postgres")
- Ensure the database has been initialized

### Database Not Found
- Wait for database initialization jobs to complete
- Check job status: `kubectl get jobs -n coinsight`

## Useful Commands

```bash
# Check cluster status
kind get clusters

# Check database pods
kubectl get pods -n coinsight | grep postgres

# Check database services
kubectl get svc -n coinsight | grep postgres

# Check database initialization jobs
kubectl get jobs -n coinsight

# View database logs
kubectl logs -n coinsight deployment/coinsight-platform-postgres-auth

# Port forward manually (if needed)
kubectl port-forward -n coinsight svc/coinsight-platform-postgres-auth 5001:5432
```

## Security Note

These configurations are for local development only. In production environments:
- Use strong passwords
- Implement proper network security
- Use SSL/TLS connections
- Restrict database access
