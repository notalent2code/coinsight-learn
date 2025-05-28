#!/bin/bash
# filepath: /home/miosha/code/bsi/ojt/coinsight/start-services.sh

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Root directory
ROOT_DIR="/home/miosha/code/bsi/ojt/coinsight"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Starting CoinSight Microservices${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to start a service in a new tab
start_service() {
    local service_name=$1
    local service_dir=$2
    
    echo -e "${YELLOW}🚀 Starting $service_name...${NC}"
    
    # Create new tab in current terminal window
    gnome-terminal --tab --title="$service_name" -- bash -c "
        cd '$ROOT_DIR/$service_dir'
        clear
        echo -e '\033[0;32mStarting $service_name...\033[0m'
        echo -e '\033[0;34mDirectory: \$(pwd)\033[0m'
        echo -e '\033[0;34m========================================\033[0m'
        mvn spring-boot:run
        echo -e '\033[0;31m$service_name has stopped. Press any key to close...\033[0m'
        read -n 1
    "
    
    echo -e "${GREEN}✅ $service_name tab created${NC}"
    sleep 1 # Small delay to ensure tab is created
}

echo -e "${BLUE}📁 Working from: $ROOT_DIR${NC}"
echo -e "\n${YELLOW}Press Enter to start all services...${NC}"
read -r

# Step 1: Start Config Server and wait
echo -e "\n${BLUE}Step 1: Starting Config Server...${NC}"
start_service "Config Server" "config-server"

echo -e "${YELLOW}⏳ Waiting 10s for Config Server to initialize...${NC}"
sleep 10

# Step 2: Start all other services in parallel
echo -e "\n${BLUE}Step 2: Starting all other services...${NC}"

start_service "Auth Service" "auth-service" &
start_service "Transaction Service" "transaction-service" &
start_service "OCR Service" "ocr-service" &
start_service "Budget Service" "budget-service" &
start_service "Notification Service" "notification-service" &
start_service "Gateway Service" "gateway-service" &

# Wait for all background jobs to complete
wait

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 All services started in tabs!${NC}"
echo -e "${GREEN}========================================${NC}"

echo -e "\n${BLUE}Service URLs:${NC}"
echo -e "• Config Server: http://localhost:8888"
echo -e "• Auth Service: http://localhost:8081"
echo -e "• Transaction Service: http://localhost:8082"
echo -e "• OCR Service: http://localhost:8083"
echo -e "• Budget Service: http://localhost:8084"
echo -e "• Notification Service: http://localhost:8085"
echo -e "• Gateway Service: http://localhost:8080"

echo -e "\n${YELLOW}💡 Each service is in its own tab in this terminal window${NC}"
echo -e "${YELLOW}💡 To stop a service: go to its tab and press Ctrl+C${NC}"
echo -e "${YELLOW}💡 To restart a service: use ./restart-service.sh <service-name>${NC}"