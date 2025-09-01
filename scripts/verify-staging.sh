#!/usr/bin/env bash
set -euo pipefail

# Verify staging readiness for Chat service
# Usage: BASE_URL=http://localhost:8083 ./scripts/verify-staging.sh

BASE_URL="${BASE_URL:-http://localhost:8083}"
echo "[batepapo] Verificando em: $BASE_URL"

curl -fsS "$BASE_URL/actuator/health" >/dev/null && echo "[batepapo] ✅ actuator/health"
curl -fsS "$BASE_URL/actuator/metrics" >/dev/null && echo "[batepapo] ✅ actuator/metrics"

code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/rest/v1/chat/health")
echo "[batepapo] chat health -> HTTP $code"

echo "[batepapo] ✅ Verificações básicas concluídas"

