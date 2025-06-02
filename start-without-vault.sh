#!/bin/bash

# Quick start script without Vault for faster development
# This script starts all services except Vault

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Coinsight services (without Vault)...${NC}"

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
echo -e "${GREEN}🎉 All services started successfully!${NC}"
echo ""

# Show service status
echo -e "${YELLOW}📊 Service Status:${NC}"
docker-compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}"

echo ""
echo -e "${BLUE}🔗 Service URLs:${NC}"
echo -e "   • Gateway Service: ${GREEN}http://localhost:8080${NC}"
echo -e "   • Config Server: ${GREEN}http://localhost:8888${NC}"
echo -e "   • Keycloak Admin: ${GREEN}http://localhost:8090${NC}"
echo -e "   • Grafana: ${GREEN}http://localhost:9091${NC} (admin/admin)"
echo -e "   • Prometheus: ${GREEN}http://localhost:9090${NC}"
echo -e "   • MailHog: ${GREEN}http://localhost:8025${NC}"

echo ""
echo -e "${YELLOW}💡 Quick commands:${NC}"
echo -e "   • View logs: ${BLUE}docker-compose logs -f [service-name]${NC}"
echo -e "   • Stop all: ${BLUE}docker-compose down${NC}"
echo -e "   • Restart service: ${BLUE}docker-compose restart [service-name]${NC}"
echo -e "   • Check health: ${BLUE}docker-compose ps${NC}"

echo ""
echo -e "${GREEN}✅ Ready for development!${NC}"
