#!/bin/bash
set -e

#######################################
# Azure Secrets Setup Script
# Creates and configures Azure Key Vault secrets
# Prerequisites: Azure CLI authenticated with Owner/Contributor role
#######################################

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Banking Account - Secrets Setup ===${NC}"

#######################################
# Configuration
#######################################

ENVIRONMENT=${1:-dev}
LOCATION="westeurope"
RESOURCE_GROUP="rg-banking-account-${ENVIRONMENT}"
KEY_VAULT_NAME="banking-kv-${ENVIRONMENT}"
SERVICE_PRINCIPAL_NAME="sp-banking-devops-${ENVIRONMENT}"

echo -e "${YELLOW}Configuration:${NC}"
echo "  Environment: ${ENVIRONMENT}"
echo "  Resource Group: ${RESOURCE_GROUP}"
echo "  Key Vault: ${KEY_VAULT_NAME}"
echo "  Service Principal: ${SERVICE_PRINCIPAL_NAME}"
echo ""

#######################################
# Step 1: Validate Prerequisites
#######################################

echo -e "${GREEN}[1/6] Validating Prerequisites...${NC}"

# Check Azure CLI
if ! command -v az &> /dev/null; then
    echo -e "${RED}Error: Azure CLI not installed${NC}"
    exit 1
fi

# Check if logged in
if ! az account show &> /dev/null; then
    echo -e "${YELLOW}Not logged in. Running 'az login'...${NC}"
    az login
fi

SUBSCRIPTION_ID=$(az account show --query id -o tsv)
TENANT_ID=$(az account show --query tenantId -o tsv)
USER_OBJECT_ID=$(az ad signed-in-user show --query id -o tsv)

echo "  Subscription ID: ${SUBSCRIPTION_ID}"
echo "  Tenant ID: ${TENANT_ID}"
echo "  User Object ID: ${USER_OBJECT_ID}"
echo ""

#######################################
# Step 2: Create Resource Group (if needed)
#######################################

echo -e "${GREEN}[2/6] Ensuring Resource Group exists...${NC}"

if az group exists --name ${RESOURCE_GROUP} | grep -q "false"; then
    az group create \
        --name ${RESOURCE_GROUP} \
        --location ${LOCATION} \
        --tags Environment=${ENVIRONMENT} Project=BankingAccount
    echo "  Resource group created"
else
    echo "  Resource group already exists"
fi
echo ""

#######################################
# Step 3: Create Key Vault
#######################################

echo -e "${GREEN}[3/6] Creating Key Vault...${NC}"

# Check if Key Vault exists
if az keyvault show --name ${KEY_VAULT_NAME} &> /dev/null; then
    echo "  Key Vault already exists"
else
    az keyvault create \
        --name ${KEY_VAULT_NAME} \
        --resource-group ${RESOURCE_GROUP} \
        --location ${LOCATION} \
        --enable-rbac-authorization false \
        --enabled-for-deployment true \
        --enabled-for-template-deployment true \
        --tags Environment=${ENVIRONMENT} Project=BankingAccount

    echo "  Key Vault created"
fi

# Grant current user full permissions
az keyvault set-policy \
    --name ${KEY_VAULT_NAME} \
    --object-id ${USER_OBJECT_ID} \
    --secret-permissions all \
    --certificate-permissions all \
    --key-permissions all

echo "  Permissions granted to current user"
echo ""

#######################################
# Step 4: Generate and Store Secrets
#######################################

echo -e "${GREEN}[4/6] Generating and Storing Secrets...${NC}"

# Generate secure password for database
DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)

echo "  Generated database password (length: ${#DB_PASSWORD})"

# Store database password
az keyvault secret set \
    --vault-name ${KEY_VAULT_NAME} \
    --name db-admin-password \
    --value "${DB_PASSWORD}" \
    --description "PostgreSQL administrator password for ${ENVIRONMENT}" \
    --tags Environment=${ENVIRONMENT}

echo "  ✓ Secret stored: db-admin-password"

# Store application secrets (example)
az keyvault secret set \
    --vault-name ${KEY_VAULT_NAME} \
    --name spring-profiles-active \
    --value "prod" \
    --description "Spring Boot active profile" \
    --tags Environment=${ENVIRONMENT}

echo "  ✓ Secret stored: spring-profiles-active"

# Generate JWT secret (example for future OAuth implementation)
JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64)

az keyvault secret set \
    --vault-name ${KEY_VAULT_NAME} \
    --name jwt-secret-key \
    --value "${JWT_SECRET}" \
    --description "JWT signing secret key" \
    --tags Environment=${ENVIRONMENT}

echo "  ✓ Secret stored: jwt-secret-key"

echo ""

#######################################
# Step 5: Create Service Principal for CI/CD
#######################################

echo -e "${GREEN}[5/6] Creating Service Principal for Azure DevOps...${NC}"

# Check if Service Principal already exists
SP_APP_ID=$(az ad sp list --display-name ${SERVICE_PRINCIPAL_NAME} --query [0].appId -o tsv)

if [ -z "${SP_APP_ID}" ]; then
    echo "  Creating new Service Principal..."

    # Create Service Principal with Contributor role on Resource Group
    SP_CREDENTIALS=$(az ad sp create-for-rbac \
        --name ${SERVICE_PRINCIPAL_NAME} \
        --role Contributor \
        --scopes /subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP} \
        --sdk-auth)

    SP_APP_ID=$(echo ${SP_CREDENTIALS} | jq -r '.clientId')
    SP_OBJECT_ID=$(az ad sp show --id ${SP_APP_ID} --query id -o tsv)

    echo "  Service Principal created"
    echo "  App ID: ${SP_APP_ID}"
    echo "  Object ID: ${SP_OBJECT_ID}"
else
    echo "  Service Principal already exists"
    echo "  App ID: ${SP_APP_ID}"
    SP_OBJECT_ID=$(az ad sp show --id ${SP_APP_ID} --query id -o tsv)

    # Get credentials (cannot retrieve existing password, user must reset if needed)
    echo -e "${YELLOW}  Note: Existing Service Principal found. If you need credentials, reset with:${NC}"
    echo "    az ad sp credential reset --id ${SP_APP_ID}"
fi

# Grant Service Principal access to Key Vault
az keyvault set-policy \
    --name ${KEY_VAULT_NAME} \
    --object-id ${SP_OBJECT_ID} \
    --secret-permissions get list \
    --certificate-permissions get list \
    --key-permissions get list

echo "  ✓ Key Vault permissions granted to Service Principal"
echo ""

#######################################
# Step 6: Display Azure DevOps Configuration
#######################################

echo -e "${GREEN}[6/6] Azure DevOps Configuration Instructions${NC}"
echo ""

if [ -n "${SP_CREDENTIALS}" ]; then
    echo -e "${BLUE}Service Connection Credentials (copy to Azure DevOps):${NC}"
    echo ""
    echo "${SP_CREDENTIALS}" | jq .
    echo ""
fi

echo -e "${BLUE}Pipeline Variables to Configure:${NC}"
echo ""
echo "Variable Group: 'banking-account-${ENVIRONMENT}'"
echo ""
echo "Variables:"
echo "  - AZURE_SUBSCRIPTION_ID: ${SUBSCRIPTION_ID}"
echo "  - AZURE_TENANT_ID: ${TENANT_ID}"
echo "  - RESOURCE_GROUP: ${RESOURCE_GROUP}"
echo "  - KEY_VAULT_NAME: ${KEY_VAULT_NAME}"
echo "  - ENVIRONMENT: ${ENVIRONMENT}"
echo ""

echo -e "${BLUE}Secret Variables (from Key Vault):${NC}"
echo "  Link Key Vault '${KEY_VAULT_NAME}' to Pipeline Variable Group"
echo "  Secrets to map:"
echo "    - db-admin-password"
echo "    - jwt-secret-key"
echo ""

#######################################
# Display Secret Values (for initial setup only)
#######################################

echo -e "${YELLOW}=== SENSITIVE INFORMATION ===${NC}"
echo -e "${YELLOW}Save these values securely and delete this output after setup:${NC}"
echo ""
echo "Database Admin Password: ${DB_PASSWORD}"
echo "Key Vault Name: ${KEY_VAULT_NAME}"
echo ""
echo -e "${YELLOW}To retrieve secrets later:${NC}"
echo "  az keyvault secret show --vault-name ${KEY_VAULT_NAME} --name db-admin-password --query value -o tsv"
echo ""

#######################################
# Summary
#######################################

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}Secrets Setup Completed!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "Next steps:"
echo "  1. Configure Azure DevOps Service Connection with Service Principal credentials"
echo "  2. Create Variable Group '${KEY_VAULT_NAME}' linked to Key Vault"
echo "  3. Run deployment pipeline"
echo ""
echo "Verification commands:"
echo "  List secrets: az keyvault secret list --vault-name ${KEY_VAULT_NAME} --query '[].name' -o table"
echo "  Test SP access: az login --service-principal -u ${SP_APP_ID} -p <password> --tenant ${TENANT_ID}"
echo ""
