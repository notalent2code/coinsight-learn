#!/bin/bash

# Enhanced Keycloak Realm and Client Initialization Script
# This script creates clients, retrieves secrets, and updates K8s secrets

set -e

# Configuration
KEYCLOAK_URL="http://localhost:8090"  # Port-forwarded Keycloak
KEYCLOAK_ADMIN_USER="admin"
KEYCLOAK_ADMIN_PASSWORD="admin"
REALM_NAME="coinsight-realm"
K8S_NAMESPACE="coinsight"

# Security setting - set to "true" only for debugging (NOT in production!)
DEBUG_SHOW_SECRETS="${DEBUG_SHOW_SECRETS:-false}"

echo "🔑 Enhanced Keycloak realm initialization: $REALM_NAME"
echo "📍 Keycloak URL: $KEYCLOAK_URL"
if [ "$DEBUG_SHOW_SECRETS" = "true" ]; then
    echo "⚠️  DEBUG MODE: Secrets will be shown in logs (INSECURE!)"
else
    echo "🔒 SECURE MODE: Secrets will be masked in logs"
fi

# Function to get admin token
get_admin_token() {
    echo "🔐 Getting admin token..."
    
    # Make API call and capture both response and HTTP status
    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$KEYCLOAK_ADMIN_USER" \
        -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=admin-cli")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" != "200" ]; then
        echo "❌ HTTP $http_code error getting admin token"
        echo "Response: $body"
        exit 1
    fi
    
    ACCESS_TOKEN=$(echo "$body" | jq -r '.access_token')
    if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
        echo "❌ Failed to get admin token from response"
        echo "Response: $body"
        exit 1
    fi
    
    if [ "$DEBUG_SHOW_SECRETS" = "true" ]; then
        echo "✅ Admin token obtained: ${ACCESS_TOKEN:0:10}...${ACCESS_TOKEN: -10}"
    else
        echo "✅ Admin token obtained"
    fi
}

# Function to check if realm exists
check_realm_exists() {
    echo "🔍 Checking if realm $REALM_NAME exists..."
    REALM_CHECK=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -w "%{http_code}" -o /dev/null)
    
    if [ "$REALM_CHECK" = "200" ]; then
        echo "✅ Realm $REALM_NAME already exists"
        return 0
    else
        echo "ℹ️  Realm $REALM_NAME does not exist"
        return 1
    fi
}

# Function to create realm (same as before)
create_realm() {
    echo "🏗️  Creating realm: $REALM_NAME"
    
    REALM_JSON='{
        "realm": "'$REALM_NAME'",
        "enabled": true,
        "registrationAllowed": true,
        "registrationEmailAsUsername": true,
        "rememberMe": true,
        "verifyEmail": false,
        "loginWithEmailAllowed": true,
        "duplicateEmailsAllowed": false,
        "resetPasswordAllowed": true,
        "editUsernameAllowed": false,
        "bruteForceProtected": true,
        "accessTokenLifespan": 3600,
        "ssoSessionIdleTimeout": 1800,
        "ssoSessionMaxLifespan": 36000,
        "defaultLocale": "en"
    }'
    
    CREATE_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$REALM_JSON" \
        -w "%{http_code}")
    
    if [ "${CREATE_RESPONSE: -3}" = "201" ]; then
        echo "✅ Realm $REALM_NAME created successfully"
    else
        echo "❌ Failed to create realm. Response: $CREATE_RESPONSE"
        exit 1
    fi
}

# Function to refresh admin token
refresh_admin_token() {
    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$KEYCLOAK_ADMIN_USER" \
        -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=admin-cli")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" != "200" ]; then
        echo "❌ Failed to refresh token - HTTP $http_code" >&2
        if [ "$DEBUG_SHOW_SECRETS" = "true" ]; then
            echo "Token refresh response: $body" >&2
        fi
        return 1
    fi
    
    ACCESS_TOKEN=$(echo "$body" | jq -r '.access_token')
    if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
        echo "❌ Failed to extract access token from refresh response" >&2
        return 1
    fi
    return 0
}

# Function to get client UUID by client ID
get_client_uuid() {
    local client_id=$1
    
    # Make API call and capture both response and HTTP status
    local response
    response=$(curl -s -w "\n%{http_code}" -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" != "200" ]; then
        echo "❌ Failed to get UUID for client: $client_id" >&2
        return 1
    fi
    
    CLIENT_UUID=$(echo "$body" | jq -r '.[0].id')
    
    if [ "$CLIENT_UUID" = "null" ] || [ -z "$CLIENT_UUID" ]; then
        echo "❌ Failed to get UUID for client: $client_id" >&2
        return 1
    fi
    
    echo "$CLIENT_UUID"
}

# Function to get client secret
get_client_secret() {
    local client_uuid=$1
    local client_id=$2
    
    # Make API call and capture both response and HTTP status
    local response
    response=$(curl -s -w "\n%{http_code}" -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients/$client_uuid/client-secret" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" != "200" ]; then
        echo "❌ Failed to get secret for client: $client_id" >&2
        return 1
    fi
    
    CLIENT_SECRET=$(echo "$body" | jq -r '.value')
    
    if [ "$CLIENT_SECRET" = "null" ] || [ -z "$CLIENT_SECRET" ]; then
        echo "❌ Failed to get secret for client: $client_id" >&2
        return 1
    fi
    
    echo "$CLIENT_SECRET"
}

# Enhanced function to create a client with authorization enabled
create_client_with_authorization() {
    local client_id=$1
    local enable_authorization=${2:-false}
    
    echo "🔧 Creating client: $client_id (Authorization: $enable_authorization)"
    
    CLIENT_JSON='{
        "clientId": "'$client_id'",
        "enabled": true,
        "publicClient": false,
        "bearerOnly": false,
        "standardFlowEnabled": true,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "authorizationServicesEnabled": '$enable_authorization',
        "redirectUris": ["*"],
        "webOrigins": ["*"],
        "attributes": {
            "access.token.lifespan": "3600"
        },
        "protocolMappers": [
            {
                "name": "audience-mapper",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-audience-mapper",
                "config": {
                    "included.client.audience": "'$client_id'",
                    "id.token.claim": "true",
                    "access.token.claim": "true"
                }
            },
            {
                "name": "roles",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-realm-role-mapper",
                "config": {
                    "user.attribute": "roles",
                    "claim.name": "roles",
                    "jsonType.label": "String",
                    "multivalued": "true",
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "userinfo.token.claim": "true"
                }
            }
        ]
    }'
    
    # Check if client already exists
    EXISTING_CLIENT=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    
    if [ "$(echo $EXISTING_CLIENT | jq '. | length')" -gt 0 ]; then
        echo "✅ Client $client_id already exists"
        return 0
    fi
    
    CREATE_CLIENT_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$CLIENT_JSON" \
        -w "%{http_code}")
    
    if [ "${CREATE_CLIENT_RESPONSE: -3}" = "201" ]; then
        echo "✅ Client $client_id created successfully"
    else
        echo "❌ Failed to create client $client_id. Response: $CREATE_CLIENT_RESPONSE"
        return 1
    fi
}

# Function to update Kubernetes secret
update_k8s_secret() {
    local secret_name=$1
    local client_id=$2
    local client_secret=$3
    
    echo "🔄 Updating K8s secret: $secret_name for client: $client_id"
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        echo "⚠️  kubectl not found, skipping K8s secret update"
        return 0
    fi
    
    # Create or update the secret
    kubectl create secret generic $secret_name \
        --from-literal=client-id="$client_id" \
        --from-literal=client-secret="$client_secret" \
        --namespace="$K8S_NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    if [ $? -eq 0 ]; then
        echo "✅ K8s secret $secret_name updated successfully"
    else
        echo "⚠️  Failed to update K8s secret $secret_name"
    fi
}

# Function to create realm roles (same as before)
create_realm_roles() {
    echo "👥 Creating realm roles..."
    
    ROLES=("ROLE_USER" "ROLE_ADMIN" "ROLE_SERVICE")
    
    for role in "${ROLES[@]}"; do
        echo "🏷️  Creating role: $role"
        
        ROLE_JSON='{
            "name": "'$role'",
            "description": "Realm role for '$role'",
            "composite": false,
            "clientRole": false
        }'
        
        CREATE_ROLE_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$ROLE_JSON" \
            -w "%{http_code}")
        
        if [ "${CREATE_ROLE_RESPONSE: -3}" = "201" ]; then
            echo "✅ Role $role created successfully"
        else
            echo "⚠️  Role $role might already exist"
        fi
    done
}

# Function to wait for Keycloak
wait_for_keycloak() {
    echo "⏳ Waiting for Keycloak to be ready..."
    
    for i in {1..30}; do
        if curl -sf "$KEYCLOAK_URL/realms/master" > /dev/null 2>&1; then
            echo "✅ Keycloak is ready!"
            return 0
        fi
        echo "⏳ Attempt $i/30: Keycloak not ready yet, waiting 5 seconds..."
        sleep 5
    done
    
    echo "❌ Keycloak did not become ready within 2.5 minutes"
    echo "💡 Make sure you have port-forwarded Keycloak:"
    echo "   kubectl port-forward svc/coinsight-platform-keycloak 8090:80 -n coinsight"
    exit 1
}

# Main execution
main() {
    echo "🚀 Starting enhanced Keycloak realm initialization..."
    
    # Check dependencies
    if ! command -v jq &> /dev/null; then
        echo "❌ jq is required but not installed."
        echo "💡 Install with: brew install jq (macOS) or apt-get install jq (Ubuntu)"
        exit 1
    fi
    
    if ! command -v kubectl &> /dev/null; then
        echo "❌ kubectl is required but not installed."
        exit 1
    fi
    
    # Wait for Keycloak to be ready
    wait_for_keycloak
    
    # Get admin token
    get_admin_token
    
    # Check if realm exists, create if not
    if ! check_realm_exists; then
        create_realm
    fi
    
    # Create realm roles
    create_realm_roles
    
    # Create microservice clients and update K8s secrets
    echo "🔧 Creating microservice clients with authorization..."
    
    # Define clients with authorization settings
    declare -A CLIENTS=(
        ["auth-service"]="true"
        ["transaction-service"]="true"
        ["budget-service"]="true"
        ["notification-service"]="true"
        ["ocr-service"]="true"
        ["coinsight-app"]="false"
    )
    
    for client_id in "${!CLIENTS[@]}"; do
        enable_auth="${CLIENTS[$client_id]}"
        
        # Create client
        if create_client_with_authorization "$client_id" "$enable_auth"; then
            # Get client UUID
            if client_uuid=$(get_client_uuid "$client_id"); then
                # Get client secret
                if client_secret=$(get_client_secret "$client_uuid" "$client_id"); then
                    # Update K8s secret
                    secret_name="${client_id}-keycloak-secret"
                    update_k8s_secret "$secret_name" "$client_id" "$client_secret"
                    
                    # Display secret based on debug mode
                    if [ "$DEBUG_SHOW_SECRETS" = "true" ]; then
                        echo "📋 Client: $client_id"
                        echo "   Secret: $client_secret (⚠️  DEBUG MODE - FULL SECRET SHOWN!)"
                        echo "   K8s Secret: $secret_name"
                    else
                        # Mask secret for security (show only first 4 and last 4 characters)
                        masked_secret="${client_secret:0:4}****${client_secret: -4}"
                        echo "📋 Client: $client_id"
                        echo "   Secret: $masked_secret (masked for security)"
                        echo "   K8s Secret: $secret_name"
                    fi
                    echo ""
                fi
            fi
        fi
    done
    
    # Also update the main keycloak secrets that your services use
    echo "🔄 Updating main Keycloak secrets..."
    
    # Get a fresh token for secret retrieval
    if ! refresh_admin_token; then
        echo "❌ Failed to get fresh token for secret retrieval"
        exit 1
    fi
    
    # Collect all secrets into an array for batch update
    declare -A SERVICE_SECRETS
    
    # Get secrets for all the microservice clients
    for client_id in "${!CLIENTS[@]}"; do
        # Skip the frontend app client
        if [ "$client_id" = "coinsight-app" ]; then
            continue
        fi
        
        if client_uuid=$(get_client_uuid "$client_id"); then
            if client_secret=$(get_client_secret "$client_uuid" "$client_id"); then
                SERVICE_SECRETS["$client_id"]="$client_secret"
                echo "✅ $client_id"
            else
                echo "⚠️  Failed to get secret for $client_id"
            fi
        else
            echo "⚠️  Failed to get UUID for $client_id"
        fi
    done
    
    # Also get the gateway client secret (coinsight-app)
    if gateway_uuid=$(get_client_uuid "coinsight-app"); then
        if gateway_secret=$(get_client_secret "$gateway_uuid" "coinsight-app"); then
            SERVICE_SECRETS["gateway-client"]="$gateway_secret"
            echo "✅ coinsight-app"
        else
            echo "⚠️  Failed to get gateway secret"
        fi
    else
        echo "⚠️  Failed to get gateway UUID"
    fi
    
    # Build the patch JSON with all secrets
    PATCH_JSON='{"data":{'
    
    # Add auth-service-secret
    if [ -n "${SERVICE_SECRETS[auth-service]}" ]; then
        AUTH_SECRET_B64=$(echo -n "${SERVICE_SECRETS[auth-service]}" | base64 -w 0)
        PATCH_JSON+='"auth-service-secret":"'$AUTH_SECRET_B64'",'
    fi
    
    # Add transaction-service-secret
    if [ -n "${SERVICE_SECRETS[transaction-service]}" ]; then
        TRANS_SECRET_B64=$(echo -n "${SERVICE_SECRETS[transaction-service]}" | base64 -w 0)
        PATCH_JSON+='"transaction-service-secret":"'$TRANS_SECRET_B64'",'
    fi
    
    # Add budget-service-secret
    if [ -n "${SERVICE_SECRETS[budget-service]}" ]; then
        BUDGET_SECRET_B64=$(echo -n "${SERVICE_SECRETS[budget-service]}" | base64 -w 0)
        PATCH_JSON+='"budget-service-secret":"'$BUDGET_SECRET_B64'",'
    fi
    
    # Add notification-service-secret
    if [ -n "${SERVICE_SECRETS[notification-service]}" ]; then
        NOTIF_SECRET_B64=$(echo -n "${SERVICE_SECRETS[notification-service]}" | base64 -w 0)
        PATCH_JSON+='"notification-service-secret":"'$NOTIF_SECRET_B64'",'
    fi
    
    # Add ocr-service-secret
    if [ -n "${SERVICE_SECRETS[ocr-service]}" ]; then
        OCR_SECRET_B64=$(echo -n "${SERVICE_SECRETS[ocr-service]}" | base64 -w 0)
        PATCH_JSON+='"ocr-service-secret":"'$OCR_SECRET_B64'",'
    fi
    
    # Add gateway-client-secret
    if [ -n "${SERVICE_SECRETS[gateway-client]}" ]; then
        GATEWAY_SECRET_B64=$(echo -n "${SERVICE_SECRETS[gateway-client]}" | base64 -w 0)
        PATCH_JSON+='"gateway-client-secret":"'$GATEWAY_SECRET_B64'",'
    fi
    
    # Remove trailing comma and close JSON
    PATCH_JSON=${PATCH_JSON%,}
    PATCH_JSON+='}}'
    
    # Apply the complete patch
    kubectl patch secret keycloak-secrets \
        --namespace="$K8S_NAMESPACE" \
        --patch="$PATCH_JSON" \
        --type=merge
    
    if [ $? -eq 0 ]; then
        echo "✅ All secrets updated in keycloak-secrets!"
    else
        echo "⚠️  Failed to update keycloak-secrets"
        return 1
    fi
    
        echo "🎉 Keycloak realm initialization completed!"
    echo ""
    echo "📋 Created clients: auth-service, transaction-service, budget-service,"
    echo "    notification-service, ocr-service, coinsight-app"
    echo ""
    echo "🔐 Secrets updated in Kubernetes secret 'keycloak-secrets'"
    echo "🌍 Keycloak admin: $KEYCLOAK_URL/admin (admin/admin)"
    echo ""
    echo "🔍 To verify: kubectl get secret keycloak-secrets -n $K8S_NAMESPACE -o yaml"
}

# Run main function
main