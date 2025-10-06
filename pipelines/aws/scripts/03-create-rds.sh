#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------------------------------------------
# This script creates a simple RDS PostgreSQL instance for development or testing.
# ⚠️ This is NOT production hardened. For production, enable Multi-AZ,
# automatic backups, encryption, and restrict public access.
# -----------------------------------------------------------------------------

AWS_REGION="${AWS_REGION:-eu-west-1}"
DB_INSTANCE_IDENTIFIER="${DB_INSTANCE_IDENTIFIER:-banking-db}"
DB_NAME="${DB_NAME:-banking}"
DB_USERNAME="${DB_USERNAME:-bankuser}"
DB_PASSWORD="${DB_PASSWORD:-changeme123}"  # ⚠️ For demo only, use Secrets Manager for real apps
DB_INSTANCE_CLASS="${DB_INSTANCE_CLASS:-db.t3.micro}"
DB_ENGINE_VERSION="${DB_ENGINE_VERSION:-15.4}"
VPC_SECURITY_GROUP_IDS="${VPC_SECURITY_GROUP_IDS:-}"   # optional, can be blank
DB_ALLOCATED_STORAGE="${DB_ALLOCATED_STORAGE:-20}"     # in GB

echo "🔸 Creating RDS PostgreSQL instance: $DB_INSTANCE_IDENTIFIER in $AWS_REGION"

CREATE_CMD=(
  aws rds create-db-instance
  --region "$AWS_REGION"
  --db-instance-identifier "$DB_INSTANCE_IDENTIFIER"
  --db-name "$DB_NAME"
  --engine postgres
  --engine-version "$DB_ENGINE_VERSION"
  --master-username "$DB_USERNAME"
  --master-user-password "$DB_PASSWORD"
  --db-instance-class "$DB_INSTANCE_CLASS"
  --allocated-storage "$DB_ALLOCATED_STORAGE"
  --backup-retention-period 0
  --publicly-accessible
)

if [[ -n "$VPC_SECURITY_GROUP_IDS" ]]; then
  CREATE_CMD+=(--vpc-security-group-ids "$VPC_SECURITY_GROUP_IDS")
fi

"${CREATE_CMD[@]}"

echo "✅ RDS creation initiated. It may take several minutes to become available."

# Wait until the DB is ready (optional but handy)
echo "⏳ Waiting for DB instance to become available..."
aws rds wait db-instance-available \
  --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --region "$AWS_REGION"

# Get the endpoint address
DB_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier "$DB_INSTANCE_IDENTIFIER" \
  --region "$AWS_REGION" \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "✅ RDS is ready."
echo "📌 Endpoint: $DB_ENDPOINT"
echo "📌 DB name: $DB_NAME"
echo "📌 Username: $DB_USERNAME"
