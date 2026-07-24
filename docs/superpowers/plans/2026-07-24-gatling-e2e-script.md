# Gatling E2E Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a shell script that orchestrates docker compose lifecycle and runs all 4 Gatling simulations as an end-to-end test.

**Architecture:** Single bash script (`scripts/gatling-e2e.sh`) with trap-based cleanup. Script tears down existing stack, builds and starts everything, health-checks the app, runs `mvn gatling:test`, then always tears down via trap.

**Tech Stack:** Bash 4+, Docker Compose v2, Maven, curl

---

### Task 1: Create gates-e2e.sh

**Files:**
- Create: `scripts/gatling-e2e.sh`

- [ ] **Step 1: Create the scripts directory and script file**

```bash
mkdir -p scripts
```

Then write `scripts/gatling-e2e.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

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

echo "[4/4] Running Gatling simulations..."
cd "$PROJECT_DIR"
mvn gatling:test
GATLING_EXIT=$?

echo ""
if [ "$GATLING_EXIT" -eq 0 ]; then
  echo -e "${GREEN}E2E PASSED${NC}"
else
  echo -e "${RED}E2E FAILED (exit code: $GATLING_EXIT)${NC}"
fi

exit $GATLING_EXIT
```

- [ ] **Step 2: Make script executable**

```bash
chmod +x scripts/gatling-e2e.sh
```

- [ ] **Step 3: Commit**

```bash
git add scripts/gatling-e2e.sh
git commit -m "feat(e2e): add Gatling E2E test script with docker compose orchestration"
```

---

### Task 2: Verify script syntax

**Files:**
- None (verification only)

- [ ] **Step 1: Check bash syntax**

```bash
bash -n scripts/gatling-e2e.sh
```

Expected: No output (syntax is valid)

- [ ] **Step 2: Commit any fixes if needed**

---

### Task 3: Update IMPLEMENTATION_STATUS.md

**Files:**
- Modify: `docs/IMPLEMENTATION_STATUS.md`

- [ ] **Step 1: Add E2E script note to Phase 9c**

After the Phase 9c completed section, add this line at the end of the bullet list:

```markdown
- `scripts/gatling-e2e.sh`: orchestrates `docker compose up`, health check,
  full Gatling simulation run, and teardown — single-command E2E test
```

- [ ] **Step 2: Commit**

```bash
git add docs/IMPLEMENTATION_STATUS.md
git commit -m "docs: add Gatling E2E script to Phase 9c status"
```
