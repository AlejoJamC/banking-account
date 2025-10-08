#!/bin/bash
set -e

#######################################
# Azure Deployment Script
# Deploys Banking Account Application to Azure
# Prerequisites: Azure CLI installed and authenticated
#######################################

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Banking Account - Azure Deployment ===${NC}"

#######################################
# Configuration Variables
#######################################

ENVIRONMENT=${1:-dev}  # dev, staging, prod (default: dev)
LOCATION="westeurope"
RESOURCE_GROUP="rg-banking-account-${ENVIRONMENT}"
ACR_NAME="bankingaccountacr${ENVIRONMENT}"
CONTAINER_APP_ENV="banking-env-${ENVIRONMENT}"
CONTAINER_APP_NAME="banking-account-${ENVIRONMENT}"
DB_SERVER_NAME="banking-db-${ENVIRONMENT}"
KEY_VAULT_NAME="banking-kv-${ENVIRONMENT}"
APP_INSIGHTS_NAME="banking-insights-${ENVIRONMENT}"

# Build configuration
BUILD_VERSION=$(date +%Y%m%d-%H%M%S)
IMAGE_TAG="${BUILD_VERSION}"
IMAGE_NAME="${ACR_NAME}.azurecr.io/banking-account:${IMAGE_TAG}"

echo -e "${YELLOW}Deployment Configuration:${NC}"
echo "  Environment: ${ENVIRONMENT}"
echo "  Location: ${LOCATION}"
echo "  Resource Group: ${RESOURCE_GROUP}"
echo "  Image Tag: ${IMAGE_TAG}"
echo ""

#######################################
# Step 1: Validate Azure CLI
#######################################

echo -e "${GREEN}[1/8] Validating Azure CLI...${NC}"

if ! command -v az &> /dev/null; then
    echo -e "${RED}Error: Azure CLI not installed${NC}"
    echo "Install from: https://aka.ms/azure-cli"
    exit 1
fi

# Check if logged in
if ! az account show &> /dev/null; then
    echo -e "${YELLOW}Not logged in. Running 'az login'...${NC}"
    az login
fi

SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo "  Using subscription: ${SUBSCRIPTION_ID}"
echo ""

#######################################
# Step 2: Create Resource Group
#######################################

echo -e "${GREEN}[2/8] Creating Resource Group...${NC}"

if az group exists --name ${RESOURCE_GROUP} | grep -q "true"; then
    echo "  Resource group already exists"
else
    az group create \
        --name ${RESOURCE_GROUP} \
        --location ${LOCATION} \
        --tags Environment=${ENVIRONMENT} Project=BankingAccount
    echo "  Resource group created"
fi
echo ""

#######################################
# Step 3: Deploy Infrastructure (Bicep)
#######################################

echo -e "${GREEN}[3/8] Deploying Infrastructure with Bicep...${NC}"

# Navigate to Bicep directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BICEP_DIR="${SCRIPT_DIR}/../bicep"
PARAMETERS_FILE="${BICEP_DIR}/parameters/${ENVIRONMENT}.parameters.json"

if [ ! -f "${PARAMETERS_FILE}" ]; then
    echo -e "${RED}Error: Parameters file not found: ${PARAMETERS_FILE}${NC}"
    exit 1
fi

# Deploy Bicep template
az deployment group create \
    --resource-group ${RESOURCE_GROUP} \
    --template-file ${BICEP_DIR}/main.bicep \
    --parameters @${PARAMETERS_FILE} \
    --parameters environment=${ENVIRONMENT} \
    --mode Incremental \
    --output table

echo "  Infrastructure deployed"
echo ""

#######################################
# Step 4: Retrieve Secrets from Key Vault
#######################################

echo -e "${GREEN}[4/8] Retrieving Database Password from Key Vault...${NC}"

# Wait for Key Vault to be ready
sleep 10

DB_PASSWORD=$(az keyvault secret show \
    --vault-name ${KEY_VAULT_NAME} \
    --name db-admin-password \
    --query value -o tsv)

if [ -z "${DB_PASSWORD}" ]; then
    echo -e "${RED}Error: Failed to retrieve database password${NC}"
    exit 1
fi

echo "  Database password retrieved"
echo ""

#######################################
# Step 5: Build Application JAR
#######################################

echo -e "${GREEN}[5/8] Building Application JAR...${NC}"

# Navigate to project root (3 levels up from scripts/)
PROJECT_ROOT="${SCRIPT_DIR}/../../.."
cd ${PROJECT_ROOT}

if [ ! -f "pom.xml" ]; then
    echo -e "${RED}Error: pom.xml not found in ${PROJECT_ROOT}${NC}"
    exit 1
fi

# Build with Maven
mvn clean package -DskipTests

if [ ! -f "target/banking-account-0.0.1.jar" ]; then
    echo -e "${RED}Error: JAR file not created${NC}"
    exit 1
fi

echo "  JAR built successfully"
echo ""

#######################################
# Step 6: Build and Push Docker Image
#######################################

echo -e "${GREEN}[6/8] Building and Pushing Docker Image...${NC}"

# Login to ACR
az acr login --name ${ACR_NAME}

# Build Docker image
docker build \
    -f docker/Dockerfile \
    -t ${IMAGE_NAME} \
    --build-arg JAR_FILE=target/banking-account-0.0.1.jar \
    .

# Push to ACR
docker push ${IMAGE_NAME}

# Also tag as 'latest' for the environment
docker tag ${IMAGE_NAME} ${ACR_NAME}.azurecr.io/banking-account:${ENVIRONMENT}-latest
docker push ${ACR_NAME}.azurecr.io/banking-account:${ENVIRONMENT}-latest

echo "  Docker image pushed: ${IMAGE_NAME}"
echo ""

#######################################
# Step 7: Deploy Container App
#######################################

echo -e "${GREEN}[7/8] Deploying Container App...${NC}"

# Get Database connection details
DB_FQDN=$(az postgres flexible-server show \
    --resource-group ${RESOURCE_GROUP} \
    --name ${DB_SERVER_NAME} \
    --query fullyQualifiedDomainName -o tsv)

DB_CONNECTION_STRING="jdbc:postgresql://${DB_FQDN}:5432/bankingdb?sslmode=require"

# Get ACR credentials
ACR_USERNAME=$(az acr credential show \
    --name ${ACR_NAME} \
    --query username -o tsv)

ACR_PASSWORD=$(az acr credential show \
    --name ${ACR_NAME} \
    --query passwords[0].value -o tsv)

# Get Application Insights connection string
APP_INSIGHTS_CONNECTION_STRING=$(az monitor app-insights component show \
    --resource-group ${RESOURCE_GROUP} \
    --app ${APP_INSIGHTS_NAME} \
    --query connectionString -o tsv)

# Update Container App with new image
az containerapp update \
    --name ${CONTAINER_APP_NAME} \
    --resource-group ${RESOURCE_GROUP} \
    --image ${IMAGE_NAME} \
    --set-env-vars \
        SPRING_PROFILES_ACTIVE=prod \
        SPRING_DATASOURCE_URL=${DB_CONNECTION_STRING} \
        SPRING_DATASOURCE_USERNAME=bankadmin \
        SPRING_DATASOURCE_PASSWORD=secretref:db-password \
        APPLICATIONINSIGHTS_CONNECTION_STRING=${APP_INSIGHTS_CONNECTION_STRING} \
    --cpu 1.0 \
    --memory 2Gi \
    --min-replicas 1 \
    --max-replicas 3

echo "  Container App updated"
echo ""

#######################################
# Step 8: Verify Deployment
#######################################

echo -e "${GREEN}[8/8] Verifying Deployment...${NC}"

# Get Container App URL
APP_URL=$(az containerapp show \
    --name ${CONTAINER_APP_NAME} \
    --resource-group ${RESOURCE_GROUP} \
    --query properties.configuration.ingress.fqdn -o tsv)

echo "  Application URL: https://${APP_URL}"
echo ""

# Wait for app to be ready
echo "  Waiting for application to start (30 seconds)..."
sleep 30

# Health check
echo "  Running health check..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://${APP_URL}/actuator/health)

if [ "${HTTP_STATUS}" -eq 200 ]; then
    echo -e "${GREEN}  ✓ Health check passed (HTTP ${HTTP_STATUS})${NC}"
else
    echo -e "${RED}  ✗ Health check failed (HTTP ${HTTP_STATUS})${NC}"
    echo ""
    echo "Check logs with:"
    echo "  az containerapp logs show --name ${CONTAINER_APP_NAME} --resource-group ${RESOURCE_GROUP} --follow"
    exit 1
fi

#######################################
# Deployment Summary
#######################################

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}Deployment Completed Successfully!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "Environment: ${ENVIRONMENT}"
echo "Application URL: https://${APP_URL}"
echo "Image: ${IMAGE_NAME}"
echo ""
echo "Next steps:"
echo "  1. Test API: curl https://${APP_URL}/actuator/health"
echo "  2. View logs: az containerapp logs show --name ${CONTAINER_APP_NAME} --resource-group ${RESOURCE_GROUP} --follow"
echo "  3. Monitor: https://portal.azure.com/#@/resource/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.App/containerApps/${CONTAINER_APP_NAME}"
echo ""

# Cleanup
cd ${SCRIPT_DIR}
