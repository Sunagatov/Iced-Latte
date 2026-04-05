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
  echo "💡 Ensure .env.prod exists in project root"
  echo "💡 Or set PROJECT_CONFIG environment variable"
  exit 1
fi

source "$CONFIG_FILE"

# Allow runtime override of Docker tag
DOCKER_TAG="${DOCKER_TAG:-latest}"

# Ensure Docker Desktop is running
if ! docker info &>/dev/null; then
  echo "🐳 Starting Docker Desktop..."
  open -a Docker
  sleep 15
  if ! docker info &>/dev/null; then
    echo "❌ Docker Desktop failed to start"
    exit 1
  fi
fi

echo "🔨 Building Docker image for ARM64 (Hetzner server architecture)..."
echo "📦 Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"

cd "$PROJECT_ROOT"

docker buildx build \
  --platform linux/arm64 \
  --no-cache \
  -t "${DOCKER_IMAGE}:${DOCKER_TAG}" \
  --load \
  .

echo "✅ Build complete: ${DOCKER_IMAGE}:${DOCKER_TAG}"
