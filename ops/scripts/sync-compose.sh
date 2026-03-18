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

# Validate SSH key
SSH_KEY_EXPANDED="${SSH_KEY/#\~/$HOME}"
if [[ ! -f "$SSH_KEY_EXPANDED" ]]; then
  echo "❌ SSH key not found: $SSH_KEY_EXPANDED"
  exit 1
fi

# Validate local compose file exists
LOCAL_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"
if [[ ! -f "$LOCAL_COMPOSE_FILE" ]]; then
  echo "❌ Local compose file not found: $LOCAL_COMPOSE_FILE"
  exit 1
fi

echo "📤 Syncing compose file to server..."
echo "🖥️  Server: ${SSH_USER}@${SSH_HOST}"
echo "📂 Remote dir: ${REMOTE_APP_DIR}"
echo "📄 File: ${REMOTE_COMPOSE_FILE}"

# Verify remote directory exists, create if needed
ssh -i "$SSH_KEY_EXPANDED" "${SSH_USER}@${SSH_HOST}" bash <<EOF
if [[ ! -d "${REMOTE_APP_DIR}" ]]; then
  echo "📁 Creating remote directory: ${REMOTE_APP_DIR}"
  mkdir -p "${REMOTE_APP_DIR}"
fi
EOF

# Copy compose file to server
scp -i "$SSH_KEY_EXPANDED" \
  "$LOCAL_COMPOSE_FILE" \
  "${SSH_USER}@${SSH_HOST}:${REMOTE_APP_DIR}/${REMOTE_COMPOSE_FILE}"

if [[ $? -eq 0 ]]; then
  echo "✅ Compose file synced successfully"
else
  echo "❌ Failed to sync compose file"
  exit 1
fi
