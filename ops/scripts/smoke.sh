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

echo "🧪 Running smoke tests..."
echo ""

# Configuration
MAX_ATTEMPTS=12
RETRY_DELAY=5
ALL_PASSED=true

# Function to check a URL
check_url() {
  local url="$1"
  local name="$2"
  local attempt=1
  
  echo "🔍 Checking $name"
  echo "   URL: $url"
  
  while [[ $attempt -le $MAX_ATTEMPTS ]]; do
    echo "   Attempt $attempt/$MAX_ATTEMPTS..."
    
    # Capture HTTP code, handle curl failures gracefully
    if HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$HEALTH_TIMEOUT" "$url" 2>/dev/null); then
      if [[ "$HTTP_CODE" == "200" ]]; then
        echo "   ✅ $name passed (HTTP $HTTP_CODE)"
        echo ""
        return 0
      else
        echo "   ⚠️  HTTP $HTTP_CODE"
      fi
    else
      echo "   ⚠️  Connection failed"
    fi
    
    if [[ $attempt -lt $MAX_ATTEMPTS ]]; then
      echo "   Retrying in ${RETRY_DELAY}s..."
      sleep $RETRY_DELAY
    fi
    
    attempt=$((attempt + 1))
  done
  
  echo "   ❌ $name failed after $MAX_ATTEMPTS attempts"
  echo ""
  return 1
}

# Check healthcheck endpoint if configured
if [[ -n "${HEALTHCHECK_URL:-}" ]]; then
  if ! check_url "$HEALTHCHECK_URL" "Health check"; then
    ALL_PASSED=false
  fi
fi

# Check smoke endpoint if configured
if [[ -n "${SMOKE_URL:-}" ]]; then
  if ! check_url "$SMOKE_URL" "Smoke test"; then
    ALL_PASSED=false
  fi
fi

# Ensure at least one check was configured
if [[ -z "${HEALTHCHECK_URL:-}" ]] && [[ -z "${SMOKE_URL:-}" ]]; then
  echo "⚠️  No health check URLs configured"
  echo "💡 Set HEALTHCHECK_URL and/or SMOKE_URL in config"
  exit 1
fi

# Final result
if [[ "$ALL_PASSED" == "true" ]]; then
  echo "✅ All smoke tests passed!"
  exit 0
else
  echo "❌ Some smoke tests failed"
  echo "💡 Check logs with: task logs"
  exit 1
fi
