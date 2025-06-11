# 🚀 Coinsight Cloud Deployment Guide

## 🎯 Quick Start Options for Jakarta, Indonesia

### Option 1: Google Cloud Platform (GKE) - FREE TIER ⭐ RECOMMENDED
**Cost: FREE for 12 months ($300 credits) + Always Free tier**
**Region: `asia-southeast2` (Jakarta)**

```bash
# 1. Setup GCP account at https://cloud.google.com/free
# 2. Create new project at https://console.cloud.google.com
# 3. Run our automated deployment script

chmod +x cloud-deploy/gcp-setup.sh
./cloud-deploy/gcp-setup.sh

# Follow the prompts to enter your Project ID
```

**Total Setup Time: ~20-30 minutes**
**Monthly Cost After Free Tier: ~$50-80 USD**

---

### Option 2: Budget VPS Deployment - CHEAPEST 💰
**Cost: $5-10/month**
**Providers: Vultr, DigitalOcean, Linode, Hetzner**

#### Recommended VPS Specs:
- **RAM**: 2GB minimum (4GB preferred)
- **CPU**: 1-2 vCPU
- **Storage**: 20GB SSD
- **Region**: Singapore/Jakarta

#### VPS Providers for Jakarta:
1. **Vultr Singapore**: $5/month (1GB RAM, 1 vCPU, 25GB SSD)
2. **DigitalOcean Singapore**: $6/month (1GB RAM, 1 vCPU, 25GB SSD)
3. **Hetzner Finland**: $4.5/month (2GB RAM, 1 vCPU, 20GB SSD) - BEST VALUE!

```bash
# After creating your VPS, SSH into it and run:
curl -fsSL https://raw.githubusercontent.com/your-repo/coinsight-learn/main/cloud-deploy/vps-deploy.sh | bash

# Or manually:
chmod +x cloud-deploy/vps-deploy.sh
./cloud-deploy/vps-deploy.sh
```

**Total Setup Time: ~30-45 minutes**
**Monthly Cost: $5-10 USD**

---

## 🛠️ Manual Deployment Steps

### Prerequisites
- Cloud account (GCP/AWS) OR VPS access
- Git repository with your code
- Basic terminal knowledge

### Step 1: Prepare Your Code
```bash
# Clone your repository to the deployment environment
git clone https://github.com/your-username/coinsight-learn.git
cd coinsight-learn

# Make scripts executable
chmod +x cloud-deploy/*.sh
```

### Step 2: Choose Your Deployment Method

#### For GCP (Free Tier):
```bash
# Install Google Cloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Run deployment
./cloud-deploy/gcp-setup.sh
```

#### For VPS:
```bash
# SSH to your VPS
ssh root@your-vps-ip

# Download and run deployment script
wget -O vps-deploy.sh https://raw.githubusercontent.com/your-repo/coinsight-learn/main/cloud-deploy/vps-deploy.sh
chmod +x vps-deploy.sh
./vps-deploy.sh
```

### Step 3: Access Your Application
After deployment completes, you'll get access URLs:
- **Gateway API**: `http://your-ip:8080` or `http://your-domain:8080`
- **Keycloak Admin**: `http://your-ip:8090` (admin/admin)

---

## 🏗️ Architecture Overview

Your deployment will include:
- **6 Microservices**: Auth, Transaction, OCR, Budget, Notification, Gateway
- **5 PostgreSQL Databases**: One per service + Keycloak
- **Kafka**: Message broker for inter-service communication
- **Redis**: Caching and session storage
- **Keycloak**: Identity and access management

## 📊 Resource Requirements

### Minimum (VPS):
- **RAM**: 2GB
- **CPU**: 1 vCPU
- **Storage**: 20GB

### Recommended (Cloud):
- **RAM**: 4-8GB
- **CPU**: 2-4 vCPU
- **Storage**: 50GB

## 🔧 Post-Deployment Configuration

### 1. Configure Domain (Optional)
```bash
# Point your domain to the external IP
# Example DNS record:
# A    coinsight.yourdomain.com    YOUR_EXTERNAL_IP
```

### 2. Enable HTTPS (Production)
```bash
# For GKE with Ingress
kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# For VPS with Let's Encrypt
sudo apt install certbot
sudo certbot --nginx -d coinsight.yourdomain.com
```

### 3. Monitor Resources
```bash
# Check pod status
kubectl get pods -n coinsight

# Check resource usage
kubectl top pods -n coinsight

# Check logs
kubectl logs -f deployment/gateway-service -n coinsight
```

## 🚨 Troubleshooting

### Common Issues:

#### 1. Out of Memory
```bash
# Scale down services temporarily
kubectl scale deployment --replicas=0 ocr-service -n coinsight
kubectl scale deployment --replicas=0 notification-service -n coinsight
```

#### 2. Database Connection Issues
```bash
# Check database pods
kubectl get pods -l app.kubernetes.io/name=postgresql -n coinsight

# Restart databases
kubectl rollout restart statefulset -n coinsight
```

#### 3. Keycloak Access Issues
```bash
# Get Keycloak external IP
kubectl get service coinsight-platform-keycloak -n coinsight

# Port forward for local access
kubectl port-forward svc/coinsight-platform-keycloak 8090:80 -n coinsight
```

## 💰 Cost Optimization Tips

### For GCP:
1. Use **Preemptible nodes** (70% cheaper)
2. Enable **Autopilot mode** for auto-scaling
3. Set up **budget alerts**
4. Use **Committed Use Discounts**

### For VPS:
1. Choose **annual billing** (usually 15-20% discount)
2. Use **shared CPU instances** for non-critical workloads
3. Enable **automatic backups** only if needed
4. Monitor resource usage and downgrade if possible

## 🔄 Maintenance

### Regular Tasks:
```bash
# Update deployments
helm upgrade coinsight-platform k8s/charts/coinsight-platform -n coinsight

# Backup databases
kubectl exec -n coinsight postgres-auth-0 -- pg_dump -U postgres auth_service > backup.sql

# Monitor disk usage
df -h
kubectl top nodes
```

### Scaling:
```bash
# Scale up for high traffic
kubectl scale deployment gateway-service --replicas=3 -n coinsight

# Scale down to save costs
kubectl scale deployment --replicas=1 --all -n coinsight
```

## 📞 Support

If you encounter issues:
1. Check the logs: `kubectl logs -f deployment/SERVICE_NAME -n coinsight`
2. Verify resources: `kubectl describe pod POD_NAME -n coinsight`
3. Check service connectivity: `kubectl get services -n coinsight`

---

## 🎉 Success Checklist

- [ ] All 6 microservices are running
- [ ] All 5 databases are healthy
- [ ] Gateway is accessible via external IP
- [ ] Keycloak admin panel is accessible
- [ ] Health check endpoints return 200 OK
- [ ] Inter-service communication works

**Congratulations! Your Coinsight platform is now running in the cloud! 🚀**
