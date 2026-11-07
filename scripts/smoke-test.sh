#!/usr/bin/env bash
# Builds the Docker image and checks it against a real PostgreSQL. Run from the host or CI.
set -euo pipefail

cd "$(dirname "$0")/.."

readonly RUN="smoke-$$"
readonly IMAGE="$RUN-app"
readonly DB_PASSWORD="$RUN"

cleanup() {
  docker rm -f "$RUN-app" "$RUN-db" >/dev/null 2>&1 || true
  docker network rm "$RUN" >/dev/null 2>&1 || true
  docker rmi "$IMAGE" >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "Smoke test failed: $1" >&2
  docker logs --tail 40 "$RUN-app" >&2 2>&1 || true
  exit 1
}

wait_until() {
  local attempts=$1
  shift
  for _ in $(seq 1 "$attempts"); do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

# Inside the container, so parallel runs do not compete for a host port.
get() { docker exec "$RUN-app" wget -qO- "http://localhost:8080$1"; }

docker build --quiet --tag "$IMAGE" . >/dev/null
docker network create "$RUN" >/dev/null

docker run --detach --name "$RUN-db" --network "$RUN" \
  --env POSTGRES_USER=smoke --env POSTGRES_PASSWORD="$DB_PASSWORD" --env POSTGRES_DB=smoke \
  postgres:18 >/dev/null
wait_until 60 docker exec "$RUN-db" pg_isready -h localhost -U smoke || fail "PostgreSQL did not start"

docker run --detach --name "$RUN-app" --network "$RUN" \
  --env SPRING_DATASOURCE_URL="jdbc:postgresql://$RUN-db:5432/smoke" \
  --env SPRING_DATASOURCE_USERNAME=smoke \
  --env SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  "$IMAGE" >/dev/null
wait_until 60 get /actuator/health || fail "the application did not become healthy"

[[ "$(get /actuator/health)" == *'"status":"UP"'* ]] || fail "health is not UP"
[[ "$(get /)" == "Hello, World!" ]] || fail "unexpected response from /"
[[ "$(get /greetings)" == *'"message":"Hello, World!"'* ]] || fail "unexpected response from /greetings"
[[ "$(docker exec "$RUN-app" id -u)" != "0" ]] || fail "the application runs as root"

echo "Smoke test passed"
