#!/bin/bash
# filepath: /home/miosha/code/bsi/ojt/coinsight/stop-services.sh

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Stopping CoinSight Microservices${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to stop service by port
stop_service_by_port() {
    local service_name=$1
    local port=$2
    
    echo -e "${YELLOW}🛑 Stopping $service_name (port $port)...${NC}"
    
    # Find process ID by port
    PID=$(lsof -ti:$port)
    
    if [ -n "$PID" ]; then
        kill -TERM $PID 2>/dev/null
        sleep 2
        
        # Check if process is still running
        if kill -0 $PID 2>/dev/null; then
            echo -e "${YELLOW}⚠️  Force killing $service_name...${NC}"
            kill -KILL $PID 2>/dev/null
        fi
        echo -e "${GREEN}✅ $service_name stopped${NC}"
    else
        echo -e "${YELLOW}⚠️  $service_name not running on port $port${NC}"
    fi
}

# Stop services by their ports (in reverse order)
stop_service_by_port "Gateway Service" "8080"
stop_service_by_port "Notification Service" "8085"
stop_service_by_port "Budget Service" "8084"
stop_service_by_port "OCR Service" "8083"
stop_service_by_port "Transaction Service" "8082"
stop_service_by_port "Auth Service" "8081"
stop_service_by_port "Config Server" "8888"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 All services have been stopped!${NC}"
echo -e "${GREEN}========================================${NC}"