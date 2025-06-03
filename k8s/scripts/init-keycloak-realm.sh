#!/bin/bash

# Enhanced Keycloak Realm and Client Initialization Script
# This script creates clients, retrieves secrets, and updates K8s secrets

set -e

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM_NAME="coinsight-realm"
K8S_NAMESPACE="coinsight"

echo "🔑 Enhanced Keycloak realm initialization: $REALM_NAME"
echo "📍 Keycloak URL: $KEYCLOAK_URL"

# Function to get admin token
get_admin_token() {
    echo "🔐 Getting admin token..."
    TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$KEYCLOAK_ADMIN_USER" \
        -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=admin-cli")
    
    ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')
    if [ "$ACCESS_TOKEN" = "null" ]; then
        echo "❌ Failed to get admin token"
        echo "Response: $TOKEN_RESPONSE"
        exit 1
    fi
    echo "✅ Admin token obtained"
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

# Function to get client UUID by client ID
get_client_uuid() {
    local client_id=$1
    echo "🔍 Getting UUID for client: $client_id"
    
    CLIENT_UUID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients?clientId=$client_id" \
        -H "Authorization: Bearer $ACCESS_TOKEN" | jq -r '.[0].id')
    
    if [ "$CLIENT_UUID" = "null" ] || [ -z "$CLIENT_UUID" ]; then
        echo "❌ Failed to get UUID for client: $client_id"
        return 1
    fi
    
    echo "✅ Client UUID: $CLIENT_UUID"
    echo "$CLIENT_UUID"
}

# Function to get client secret
get_client_secret() {
    local client_uuid=$1
    local client_id=$2
    
    echo "🔐 Getting secret for client: $client_id"
    
    SECRET_RESPONSE=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients/$client_uuid/client-secret" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    
    CLIENT_SECRET=$(echo $SECRET_RESPONSE | jq -r '.value')
    
    if [ "$CLIENT_SECRET" = "null" ] || [ -z "$CLIENT_SECRET" ]; then
        echo "❌ Failed to get secret for client: $client_id"
        return 1
    fi
    
    echo "✅ Retrieved secret for client: $client_id"
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

# Function to wait for Keycloak (same as before)
wait_for_keycloak() {
    echo "⏳ Waiting for Keycloak to be ready..."
    
    for i in {1..30}; do
        if curl -s -f "$KEYCLOAK_URL/realms/master" > /dev/null 2>&1; then
            echo "✅ Keycloak is ready"
            return 0
        fi
        echo "⏳ Attempt $i/30: Keycloak not ready yet, waiting 10 seconds..."
        sleep 10
    done
    
    echo "❌ Keycloak did not become ready within 5 minutes"
    exit 1
}

# Main execution
main() {
    echo "🚀 Starting enhanced Keycloak realm initialization..."
    
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
                    
                    echo "📋 Client: $client_id"
                    echo "   Secret: $client_secret"
                    echo "   K8s Secret: $secret_name"
                    echo ""
                fi
            fi
        fi
    done
    
    # Also update the main keycloak secrets that your services use
    echo "🔄 Updating main Keycloak secrets..."
    
    # Get secrets for the specific clients your services expect
    if auth_uuid=$(get_client_uuid "auth-service"); then
        if auth_secret=$(get_client_secret "$auth_uuid" "auth-service"); then
            kubectl patch secret keycloak-secrets \
                --namespace="$K8S_NAMESPACE" \
                --patch="{\"data\":{\"auth-service-secret\":\"$(echo -n $auth_secret | base64)\"}}" 2>/dev/null || echo "⚠️  Could not update auth-service-secret"
        fi
    fi
    
    if trans_uuid=$(get_client_uuid "transaction-service"); then
        if trans_secret=$(get_client_secret "$trans_uuid" "transaction-service"); then
            kubectl patch secret keycloak-secrets \
                --namespace="$K8S_NAMESPACE" \
                --patch="{\"data\":{\"transaction-service-secret\":\"$(echo -n $trans_secret | base64)\"}}" 2>/dev/null || echo "⚠️  Could not update transaction-service-secret"
        fi
    fi
    
    echo "🎉 Enhanced Keycloak realm initialization completed!"
    echo "📋 Summary:"
    echo "   - Realm: $REALM_NAME"
    echo "   - Roles: ROLE_USER, ROLE_ADMIN, ROLE_SERVICE"
    echo "   - Clients with Authorization: auth-service, transaction-service, budget-service, notification-service, ocr-service"
    echo "   - Public Client: coinsight-app"
    echo "   - K8s Secrets: Updated automatically"
    echo "   - Access: $KEYCLOAK_URL/realms/$REALM_NAME"
}

# Check dependencies
if ! command -v jq &> /dev/null; then
    echo "❌ jq is required but not installed. Please install jq and try again."
    exit 1
fi

# Run main function
main