#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------------------------------------------
# This script creates an AWS App Runner service based on an existing ECR image.
# Make sure you have already:
#   1. Pushed a Docker image to ECR (Step 1)
#   2. Created an RDS PostgreSQL instance (Step 2)
# -----------------------------------------------------------------------------

AWS_REGION="${AWS_REGION:-eu-west-1}"
SERVICE_NAME="${SERVICE_NAME:-banking-app}"
ECR_REPOSITORY="${ECR_REPOSITORY:-banking-account}"
IMAGE_TAG="${IMAGE_TAG:-latest}"   # You can pass a Git SHA or 'latest'
CPU="${CPU:-0.25}"                 # vCPU (0.25, 0.5, 1)
MEMORY="${MEMORY:-512}"           # MB (512, 1024, 2048)
DB_ENDPOINT="${DB_ENDPOINT:-}"    # required
DB_NAME="${DB_NAME:-banking}"
DB_USERNAME="${DB_USERNAME:-bankuser}"
DB_PASSWORD="${DB_PASSWORD:-changeme123}"

if [[ -z "$DB_ENDPOINT" ]]; then
  echo "❌ DB_ENDPOINT is required. Provide it as an env variable."
  exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_IMAGE_URI="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo "🔸 Creating App Runner service: $SERVICE_NAME"
echo "   Using image: $ECR_IMAGE_URI"

aws apprunner create-service \
  --region "$AWS_REGION" \
  --service-name "$SERVICE_NAME" \
  --source-configuration "{
    \"ImageRepository\": {
      \"ImageIdentifier\": \"${ECR_IMAGE_URI}\",
      \"ImageRepositoryType\": \"ECR\",
      \"ImageConfiguration\": {
        \"Port\": \"8080\",
        \"RuntimeEnvironmentVariables\": {
          \"SPRING_PROFILES_ACTIVE\": \"prod\",
          \"SPRING_DATASOURCE_URL\": \"jdbc:postgresql://${DB_ENDPOINT}:5432/${DB_NAME}\",
          \"SPRING_DATASOURCE_USERNAME\": \"${DB_USERNAME}\",
          \"SPRING_DATASOURCE_PASSWORD\": \"${DB_PASSWORD}\"
        }
      }
    },
    \"AutoDeploymentsEnabled\": true
  }" \
  --instance-configuration "{
    \"Cpu\": \"${CPU}\",
    \"Memory\": \"${MEMORY}\"
  }"

echo "✅ App Runner service creation requested. This may take a few minutes."
echo "ℹ️ Run 'aws apprunner list-services' to see the status."
