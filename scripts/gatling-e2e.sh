#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

to_docker_path() {
  local path="$1"
  if command -v cygpath &>/dev/null; then
    cygpath -w "$path"
  elif command -v wslpath &>/dev/null; then
    wslpath -w "$path"
  else
    echo "$path"
  fi
}
DOCKER_PROJECT_DIR="$(to_docker_path "$PROJECT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

cleanup() {
  echo ""
  echo "Tearing down docker compose..."
  docker compose -f "$PROJECT_DIR/docker-compose.yml" down --volumes 2>/dev/null || true
}
trap cleanup EXIT

echo "=== Ledgerly Gatling E2E Test ==="
echo ""

echo "[1/4] Tearing down any existing containers..."
docker compose -f "$PROJECT_DIR/docker-compose.yml" down --volumes 2>/dev/null || true

echo "[2/4] Starting docker compose stack..."
docker compose -f "$PROJECT_DIR/docker-compose.yml" up --build -d

echo "[3/4] Waiting for app to become healthy..."
for i in $(seq 1 30); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/actuator/health 2>/dev/null | grep -q "200"; then
    echo "App is healthy (attempt $i)"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo -e "${RED}App did not become healthy within 60s${NC}"
    exit 1
  fi
  sleep 2
done

echo "[4/4] Running Gatling simulations (via Docker Maven)..."
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$DOCKER_PROJECT_DIR:/app" \
  -v maven-cache:/root/.m2 \
  -w /app \
  maven:3.9.9-eclipse-temurin-21 \
  mvn -B -ntp gatling:test -DledgerlyBaseUrl=http://host.docker.internal:8080/api
GATLING_EXIT=$?

echo ""
if [ "$GATLING_EXIT" -eq 0 ]; then
  echo -e "${GREEN}E2E PASSED${NC}"
else
  echo -e "${RED}E2E FAILED (exit code: $GATLING_EXIT)${NC}"
fi

exit $GATLING_EXIT
