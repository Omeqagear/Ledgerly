# Gatling E2E Test Script

**Date:** 2026-07-24
**Status:** Design approved

## Overview

A shell script that orchestrates the full E2E Gatling test cycle: start docker
compose stack, wait for healthy, run all 4 Gatling simulations, verify assertions,
tear down.

## File

`scripts/gatling-e2e.sh` — single script, no other changes.

## Flow

1. `docker compose down --volumes` — ensures clean DB, no stale data
2. `docker compose up --build -d` — builds and starts app, postgres, redis, prometheus
3. Health check — loops `curl -s http://localhost:8080/api/actuator/health`
   every 2 seconds, max 30 attempts (60s). Fails if never healthy
4. `mvn gatling:test` — runs all simulations in `src/test/scala/`
   (Customer, InvoicePayment, Reporting, MixedWorkload)
5. Exit code check: 0 = all assertions passed, non-zero = at least one failed
6. `docker compose down` — always runs, even on failure
7. Prints `E2E PASSED` or `E2E FAILED (exit code: N)` as last line

## Prerequisites

- Docker (docker compose v2)
- Maven (`mvn`) on PATH
- Bash 4+ or compatible shell
- `curl` on PATH

## Usage

```bash
bash scripts/gatling-e2e.sh
```

## Error handling

- Docker daemon not running → fail immediately with message
- Health check timeout → fail with "App did not become healthy"
- Gatling assertion failure → capture exit code, continue to teardown, report failure
- Script interrupted (Ctrl+C) → trap ensures `docker compose down` runs

## Not in scope

- CI/CD integration (GitHub Actions workflow)
- Parameterization (always runs all 4 simulations)
- Report archiving or comparison
