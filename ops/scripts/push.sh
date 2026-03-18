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

# Ensure Docker is running
if ! docker info &>/dev/null; then
  echo "❌ Docker is not running. Start Docker Desktop first."
  exit 1
fi

echo "🚀 Pushing Docker image to Docker Hub..."
echo "📦 Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"

docker push "${DOCKER_IMAGE}:${DOCKER_TAG}"

echo "✅ Push complete: ${DOCKER_IMAGE}:${DOCKER_TAG}"
