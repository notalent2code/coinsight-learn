#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Coinsight services with Vault...${NC}"

# Enable BuildKit
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

echo -e "${YELLOW}📦 Starting infrastructure services...${NC}"

# Start infrastructure services first
docker-compose up -d \
  postgres \
  auth-db \
  transaction-db \
  budget-db \
  notification-db \
  keycloak \
  zookeeper \
  kafka \
  redis \
  prometheus \
  grafana \
  mailhog

echo -e "${YELLOW}⏳ Waiting for infrastructure to be ready...${NC}"
sleep 10

echo -e "${YELLOW}🔐 Starting Vault...${NC}"
docker-compose up -d vault

echo -e "${YELLOW}⏳ Waiting for Vault to be ready...${NC}"
sleep 10

echo -e "${YELLOW}🔑 Initializing Vault with secrets...${NC}"
# Execute Vault initialization inside the container
docker-compose exec -T vault sh -c '
  export VAULT_ADDR="http://localhost:8200"
  export VAULT_TOKEN="myroot"
  
  echo "Waiting for Vault to be ready..."
  until vault status >/dev/null 2>&1; do
    sleep 2
  done
  
  echo "Enabling KV secrets engine..."
  vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV engine already enabled"
  
  echo "Storing secrets..."
  vault kv put secret/auth-service keycloak.client.secret="OgRK7as6bjDxNKT5qpAbAtaMKsg7c1bh"
  vault kv put secret/transaction-service keycloak.client.secret="61fkBYf9jBS4tT3hcSYtoKA75pr0BcKn"
  vault kv put secret/budget-service keycloak.client.secret="WiDKoqXBrEbkDjc6viFlAIHZFmu2M7vg"
  vault kv put secret/notification-service keycloak.client.secret="HmPlEAIi4rJA5VZAnCX822wEjdXaXc8o"
  vault kv put secret/gateway-service keycloak.client.secret="qjcqpzF7tYmhGitMMQXOIJcmVJrJ2xPC"
  vault kv put secret/ocr-service keycloak.client.secret="epNXpJWhzUqwH87VxspGLXh2hgC2mBkn"
  
  echo "✅ Vault initialization completed!"
  echo "Stored secrets:"
  vault kv list secret/
'

echo -e "${YELLOW}🔧 Starting config-server...${NC}"
docker-compose up -d config-server

echo -e "${YELLOW}⏳ Waiting for config-server to be ready...${NC}"
sleep 15

echo -e "${YELLOW}🚀 Starting microservices...${NC}"
docker-compose up -d \
  auth-service \
  transaction-service \
  ocr-service \
  budget-service \
  notification-service

echo -e "${YELLOW}⏳ Waiting for microservices to be ready...${NC}"
sleep 10

echo -e "${YELLOW}🌐 Starting gateway-service...${NC}"
docker-compose up -d gateway-service

echo ""
echo -e "${GREEN}🎉 All services started successfully with Vault!${NC}"
echo ""

# Show service status
echo -e "${YELLOW}📊 Service Status:${NC}"
docker-compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}"

echo ""
echo -e "${BLUE}🔗 Service URLs:${NC}"
echo -e "   • Gateway Service: ${GREEN}http://localhost:8080${NC}"
echo -e "   • Config Server: ${GREEN}http://localhost:8888${NC}"
echo -e "   • Vault UI: ${GREEN}http://localhost:8200${NC} (Token: myroot)"
echo -e "   • Keycloak Admin: ${GREEN}http://localhost:8090${NC}"
echo -e "   • Grafana: ${GREEN}http://localhost:9091${NC} (admin/admin)"
echo -e "   • Prometheus: ${GREEN}http://localhost:9090${NC}"
echo -e "   • MailHog: ${GREEN}http://localhost:8025${NC}"

echo ""
echo -e "${YELLOW}💡 Quick commands:${NC}"
echo -e "   • View logs: ${BLUE}docker-compose logs -f [service-name]${NC}"
echo -e "   • Stop all: ${BLUE}docker-compose down${NC}"
echo -e "   • Restart service: ${BLUE}docker-compose restart [service-name]${NC}"
echo -e "   • Check Vault secrets: ${BLUE}vault kv list secret/${NC}"

echo ""
echo -e "${GREEN}✅ Ready for development with Vault!${NC}"