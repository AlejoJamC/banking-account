#!/bin/bash
set -e

#######################################
# Azure Cleanup Script
# Deletes all resources for an environment
# WARNING: This is destructive!
#######################################

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ENVIRONMENT=${1:-dev}
RESOURCE_GROUP="rg-banking-account-${ENVIRONMENT}"

echo -e "${RED}=== WARNING: Resource Cleanup ===${NC}"
echo ""
echo "This will DELETE all resources in:"
echo "  Resource Group: ${RESOURCE_GROUP}"
echo ""
read -p "Are you sure? Type 'DELETE' to confirm: " CONFIRM

if [ "${CONFIRM}" != "DELETE" ]; then
    echo "Cleanup cancelled"
    exit 0
fi

echo ""
echo -e "${YELLOW}Deleting resource group...${NC}"

az group delete \
    --name ${RESOURCE_GROUP} \
    --yes \
    --no-wait

echo -e "${GREEN}Deletion initiated (running in background)${NC}"
echo ""
echo "Monitor with:"
echo "  az group show --name ${RESOURCE_GROUP}"
