#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-eu-west-1}"
REPO_NAME="${REPO_NAME:-banking-account}"
IMAGE_TAG="${IMAGE_TAG:-$(git rev-parse --short HEAD)}"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"

echo "🔸 Logging in ECR..."
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_URI"

echo "🔸 Building docker image..."
docker build -t "${REPO_NAME}:${IMAGE_TAG}" -f docker/Dockerfile .

echo "🔸 Tagging image..."
docker tag "${REPO_NAME}:${IMAGE_TAG}" "${ECR_URI}:${IMAGE_TAG}"

echo "🚀 Pushing image to ECR..."
docker push "${ECR_URI}:${IMAGE_TAG}"

echo "✅ Image uploaded with tag: ${IMAGE_TAG}"
echo "📌 ECR Image URI: ${ECR_URI}:${IMAGE_TAG}"
