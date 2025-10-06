#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-eu-west-1}"
REPO_NAME="${REPO_NAME:-banking-account}"

echo "🔸 Verifying if ECR repositories exists: $REPO_NAME"
if aws ecr describe-repositories --repository-names "$REPO_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
  echo "✅ ECR already exists: $REPO_NAME"
else
  echo "🆕 Creating ECR Repository ..."
  aws ecr create-repository --repository-name "$REPO_NAME" --region "$AWS_REGION" \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
  echo "✅ ECR repository created"
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"

echo "📌 ECR URI: $ECR_URI"
