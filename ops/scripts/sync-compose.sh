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

LOCAL_COMPOSE_FILE="$PROJECT_ROOT/maintaner/deployment/docker-compose.prod.yml"
if [[ ! -f "$LOCAL_COMPOSE_FILE" ]]; then
  echo "❌ Local compose file not found: $LOCAL_COMPOSE_FILE"
  exit 1
fi

# 📌 SOURCE OF TRUTH: .env.prod is read from Vault, not from this project.
# Vault path: /Users/zufar/IdeaProjects/Vault/hetzner/apps/iced-latte/.env.prod
VAULT_ENV_FILE="/Users/zufar/IdeaProjects/Vault/hetzner/apps/iced-latte/.env.prod"
if [[ -f "$VAULT_ENV_FILE" ]]; then
  LOCAL_ENV_FILE="$VAULT_ENV_FILE"
else
  # Fallback to project-local copy if Vault is not available
  LOCAL_ENV_FILE="$PROJECT_ROOT/maintaner/deployment/.env.prod"
  echo "⚠️  Vault not found, falling back to local .env.prod"
fi
if [[ ! -f "$LOCAL_ENV_FILE" ]]; then
  echo "❌ .env.prod not found at: $LOCAL_ENV_FILE"
  exit 1
fi

echo "📤 Syncing compose file and .env.prod to server..."
echo "🖥️  Server: ${SSH_USER}@${SSH_HOST}"
echo "📂 Remote dir: ${REMOTE_APP_DIR}"

ssh -i "$SSH_KEY_EXPANDED" "${SSH_USER}@${SSH_HOST}" bash <<EOF
if [[ ! -d "${REMOTE_APP_DIR}" ]]; then
  echo "📁 Creating remote directory: ${REMOTE_APP_DIR}"
  mkdir -p "${REMOTE_APP_DIR}"
fi
EOF

scp -i "$SSH_KEY_EXPANDED" \
  "$LOCAL_COMPOSE_FILE" \
  "${SSH_USER}@${SSH_HOST}:${REMOTE_APP_DIR}/${REMOTE_COMPOSE_FILE}"

scp -i "$SSH_KEY_EXPANDED" \
  "$LOCAL_ENV_FILE" \
  "${SSH_USER}@${SSH_HOST}:${REMOTE_APP_DIR}/.env.prod"

echo "✅ Compose file and .env.prod synced successfully"
