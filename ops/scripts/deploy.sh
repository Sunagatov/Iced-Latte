#!/usr/bin/env bash
set -euo pipefail

# Load config with environment override support
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="${PROJECT_CONFIG:-.env.prod}"

# Resolve relative path if needed
if [[ "$CONFIG_FILE" != /* ]]; then
  CONFIG_FILE="$PROJECT_ROOT/$CONFIG_FILE"
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "❌ Config file not found: $CONFIG_FILE"
  exit 1
fi

source "$CONFIG_FILE"

# Allow runtime override of Docker tag
DOCKER_TAG="${DOCKER_TAG:-latest}"

# Validate SSH key
SSH_KEY_EXPANDED="${SSH_KEY/#\~/$HOME}"
if [[ ! -f "$SSH_KEY_EXPANDED" ]]; then
  echo "❌ SSH key not found: $SSH_KEY_EXPANDED"
  exit 1
fi

echo "🚢 Deploying to Hetzner server..."
echo "🖥️  Server: ${SSH_USER}@${SSH_HOST}"
echo "📦 Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
echo "📂 Remote dir: ${REMOTE_APP_DIR}"
echo "📄 Compose file: ${REMOTE_COMPOSE_FILE}"
echo ""

# Deploy via SSH using docker-compose
ssh -i "$SSH_KEY_EXPANDED" "${SSH_USER}@${SSH_HOST}" bash <<EOF
set -euo pipefail

# Verify remote app directory exists
if [[ ! -d "${REMOTE_APP_DIR}" ]]; then
  echo "❌ Remote app directory does not exist: ${REMOTE_APP_DIR}"
  echo "💡 Create it with: mkdir -p ${REMOTE_APP_DIR}"
  exit 1
fi

cd ${REMOTE_APP_DIR}

# Verify compose file exists
if [[ ! -f "${REMOTE_COMPOSE_FILE}" ]]; then
  echo "❌ Compose file not found: ${REMOTE_APP_DIR}/${REMOTE_COMPOSE_FILE}"
  echo "💡 Sync it with: task sync-compose"
  exit 1
fi

echo "📥 Pulling latest image..."
export DOCKER_IMAGE=${DOCKER_IMAGE}
export DOCKER_TAG=${DOCKER_TAG}

if ! docker compose version >/dev/null 2>&1; then
  echo "❌ Docker Compose v2 is not available on remote server"
  exit 1
fi

docker compose -f ${REMOTE_COMPOSE_FILE} pull

echo "🚀 Recreating containers..."
docker compose -f ${REMOTE_COMPOSE_FILE} up -d --remove-orphans

echo "⏳ Waiting for containers to stabilize..."
sleep 5

echo "📊 Container status:"
docker compose -f ${REMOTE_COMPOSE_FILE} ps

echo ""
echo "✅ Deployment complete"
EOF

echo ""
echo "💡 Check logs with: task logs"
echo "💡 Run smoke test with: task smoke"
