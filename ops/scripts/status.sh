#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="${PROJECT_CONFIG:-.env.prod}"

if [[ "$CONFIG_FILE" != /* ]]; then
  CONFIG_FILE="$PROJECT_ROOT/$CONFIG_FILE"
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "❌ Config file not found: $CONFIG_FILE"
  exit 1
fi

source "$CONFIG_FILE"

SSH_KEY_EXPANDED="${SSH_KEY/#\~/$HOME}"
if [[ ! -f "$SSH_KEY_EXPANDED" ]]; then
  echo "❌ SSH key not found: $SSH_KEY_EXPANDED"
  exit 1
fi

echo "📊 Container status on ${SSH_USER}@${SSH_HOST}..."
echo ""

ssh -i "$SSH_KEY_EXPANDED" "${SSH_USER}@${SSH_HOST}" \
  "cd ${REMOTE_APP_DIR} && docker compose -f ${REMOTE_COMPOSE_FILE} ps"
