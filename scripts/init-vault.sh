# #!/bin/bash

# # Set Vault address and token
# export VAULT_ADDR="http://localhost:8200"
# export VAULT_TOKEN="myroot"

# echo "🔐 Initializing Vault with secrets..."

# # Wait for Vault to be ready
# echo "⏳ Waiting for Vault to be ready..."
# until vault status >/dev/null 2>&1; do
#   echo "Vault not ready, waiting..."
#   sleep 2
# done

# echo "✅ Vault is ready!"

# # Enable KV secrets engine (if not already enabled)
# echo "📝 Enabling KV secrets engine..."
# vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV engine already enabled"

# # Store Keycloak client secrets for each service
# echo "🔑 Storing Keycloak client secrets..."

# vault kv put secret/auth-service \
#   keycloak.client.secret=bDYvlPVeFqkGbENGJKRbpH9iHY9P6Fcs

# vault kv put secret/transaction-service \
#   keycloak.client.secret=M0F02OtnXlRVdcWd1ygFjnUsQ99sytyK

# vault kv put secret/budget-service \
#   keycloak.client.secret=5eI8icRz6i486Vw8ikeR9PvM45nwG8rV

# vault kv put secret/notification-service \
#   keycloak.client.secret=3Ty5tiloZ2zS9qNkJkly7oiic3AITrSD

# vault kv put secret/gateway-service \
#   keycloak.client.secret=IvZbEpwjj2NoO9LRn7xSp3Z1SbrRcNZy

# vault kv put secret/ocr-service \
#   keycloak.client.secret=IXwo21bOQUGa7yilhnhsZixCtHHdFjdG

# # # Store other sensitive configuration
# # echo "🔐 Storing additional secrets..."

# # vault kv put secret/application \
# #   db.password=postgres \
# #   jwt.secret=mySecretKey123!@# \
# #   mail.password=mail_password

# echo "✅ Vault initialization completed!"
# echo ""
# echo "🔍 Verify secrets:"
# vault kv list secret/