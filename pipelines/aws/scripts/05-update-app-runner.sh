#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------------------------------------------
# Update or redeploy an existing AWS App Runner service.
#
# Usage:
#   # Force a redeploy with the SAME image/config (no new tag):
#   AWS_REGION=eu-west-1 SERVICE_NAME=banking-app bash aws/scripts/05-update-app-runner.sh
#
#   # Update to a NEW image tag from ECR and deploy it:
#   AWS_REGION=eu-west-1 SERVICE_NAME=banking-app ECR_REPOSITORY=banking-account IMAGE_TAG=abc1234 \
#     bash aws/scripts/05-update-app-runner.sh
#
# Behavior:
#   - If IMAGE_TAG is provided, it updates the App Runner service to use the new
#     ECR image tag via `update-service`.
#   - If IMAGE_TAG is NOT provided, it triggers a `start-deployment` to redeploy
#     the current configuration (useful when env vars changed via console/SSM).
# -----------------------------------------------------------------------------

AWS_REGION="${AWS_REGION:-eu-west-1}"
SERVICE_NAME="${SERVICE_NAME:-banking-app}"
ECR_REPOSITORY="${ECR_REPOSITORY:-banking-account}"
IMAGE_TAG="${IMAGE_TAG:-}"  # optional

# Resolve Service ARN from name
SERVICE_ARN=$(aws apprunner list-services \
  --region "$AWS_REGION" \
  --query "ServiceSummaryList[?ServiceName=='$SERVICE_NAME'].ServiceArn | [0]" \
  --output text)

if [[ -z "$SERVICE_ARN" || "$SERVICE_ARN" == "None" ]]; then
  echo "❌ Service '$SERVICE_NAME' not found in region '$AWS_REGION'"
  exit 1
fi

echo "🔸 Target App Runner service: $SERVICE_NAME"
echo "   ARN: $SERVICE_ARN"

if [[ -n "$IMAGE_TAG" ]]; then
  echo "🔸 Updating service image to tag: $IMAGE_TAG"
  ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
  ECR_IMAGE_URI="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}"

  aws apprunner update-service \
    --region "$AWS_REGION" \
    --service-arn "$SERVICE_ARN" \
    --source-configuration "{
      \"ImageRepository\": {
        \"ImageIdentifier\": \"${ECR_IMAGE_URI}\",
        \"ImageRepositoryType\": \"ECR\"
      },
      \"AutoDeploymentsEnabled\": true
    }"

  echo "✅ Update requested with image: $ECR_IMAGE_URI"
else
  echo "🔸 No IMAGE_TAG provided. Forcing a new deployment of the current config..."
  aws apprunner start-deployment \
    --region "$AWS_REGION" \
    --service-arn "$SERVICE_ARN"
  echo "✅ Deployment triggered with existing configuration"
fi

# Optional: show current service state
aws apprunner describe-service \
  --region "$AWS_REGION" \
  --service-arn "$SERVICE_ARN" \
  --query 'Service.Status'
