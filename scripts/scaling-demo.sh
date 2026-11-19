#!/usr/bin/env bash
# Shows that a service can run as several instances and that the gateway spreads requests over
# them. It scales the service, calls the same endpoint many times through the frontend (the way a
# browser does), reads the X-Served-By header that every service adds, and counts how many requests
# each instance answered.
#
#   ./scripts/scaling-demo.sh                          # reservation-service, 3 instances, 60 requests
#   ./scripts/scaling-demo.sh resident-service 2       # another service and instance count
#   ./scripts/scaling-demo.sh reservation-service 3 --kill-one   # also stop one and call again
#
# Needs the stack from `docker compose up -d` (it can be running already), curl, and the board
# password: it is read from BOARD_SEED_PASSWORD in .env, or give it as BOARD_PASSWORD=... when you
# run the script.
set -euo pipefail
cd "$(dirname "$0")/.."

SERVICE="${1:-reservation-service}"
INSTANCES="${2:-3}"
KILL_ONE="no"
[ "${3:-}" = "--kill-one" ] && KILL_ONE="yes"
REQUESTS="${REQUESTS:-60}"

# Read a value from .env as text. Sourcing the file would let the shell interpret characters such as
# $, # and quotes in a password differently than Docker Compose does.
env_value() {
  [ -f .env ] || return 0
  grep -E "^$1=" .env | tail -1 | cut -d= -f2- | sed -E "s/^\"(.*)\"$/\1/; s/^'(.*)'$/\1/" || true
}
PORT="${FRONTEND_PORT:-$(env_value FRONTEND_PORT)}"; PORT="${PORT:-3000}"
BOARD_PASSWORD="${BOARD_PASSWORD:-$(env_value BOARD_SEED_PASSWORD)}"
[ -n "$BOARD_PASSWORD" ] || { echo "No board password. Set BOARD_SEED_PASSWORD in .env, or run: BOARD_PASSWORD=... $0"; exit 1; }
BASE="http://localhost:${PORT}"
PERIOD="$(date +%Y-%m)"

case "$SERVICE" in
  reservation-service) ENDPOINT="/api/resources" ;;
  resident-service)    ENDPOINT="/api/buildings" ;;
  billing-service)     ENDPOINT="/api/invoices/overview?period=${PERIOD}" ;;
  maintenance-service) ENDPOINT="/api/maintenance/open" ;;
  *) echo "Unknown service '$SERVICE'. Use reservation-service, resident-service, billing-service or maintenance-service."; exit 1 ;;
esac

step() { printf '\n== %s\n' "$1"; }

step "1. Run ${INSTANCES} instances of ${SERVICE}"
docker compose up -d --scale "${SERVICE}=${INSTANCES}" --no-recreate >/dev/null
printf 'Waiting for the instances to become healthy'
for _ in $(seq 1 90); do
  healthy=$(docker compose ps --format '{{.Name}} {{.Health}}' "$SERVICE" | grep -c healthy || true)
  [ "$healthy" -ge "$INSTANCES" ] && break
  printf '.'; sleep 2
done
echo
docker compose ps --format 'table {{.Name}}\t{{.Status}}' "$SERVICE"
echo "(Consul shows the same instances: http://localhost:8500, Services -> ${SERVICE})"

step "2. Sign in as the board"
printf 'Waiting for the frontend'
for _ in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "${BASE}/" || true)" = "200" ] && break
  printf '.'; sleep 2
done
echo
# Escape \ and " so any password is valid inside the JSON string.
JSON_PASSWORD=$(printf '%s' "$BOARD_PASSWORD" | sed 's/\\/\\\\/g; s/"/\\"/g')
RESPONSE=$(curl -s -w '\n%{http_code}' -X POST "${BASE}/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"board@example.com\",\"password\":\"${JSON_PASSWORD}\"}" || true)
CODE="${RESPONSE##*$'\n'}"
BODY="${RESPONSE%$'\n'*}"
TOKEN=$(printf '%s' "$BODY" | sed -nE 's/.*"token" *: *"([^"]+)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "Login failed (HTTP ${CODE:-none})."
  case "$CODE" in
    401) echo "The password was refused. The board account keeps the password it was created with, so if"
         echo ".env changed after the first start, run: docker compose down -v   (this deletes the data)."
         echo "Or give the right one: BOARD_PASSWORD=... $0" ;;
    429) echo "Too many login attempts from this address. Wait a minute and run the script again." ;;
    000|502|503|504) echo "The gateway or resident-service isn't ready yet. Check: docker compose ps" ;;
    *) echo "Check: docker compose logs gateway resident-service" ;;
  esac
  exit 1
fi

distribution() {
  step "$1"
  echo "GET ${ENDPOINT}, ${REQUESTS} times"
  for _ in $(seq 1 "$REQUESTS"); do
    # The gateway adds its own X-Served-By too; keep the one from the service.
    curl -s -D - -o /dev/null "${BASE}${ENDPOINT}" -H "Authorization: Bearer ${TOKEN}" \
      | tr -d '\r' | awk -v svc="${SERVICE}@" 'tolower($1)=="x-served-by:" && index($2, svc)==1 {print $2}'
  done | sort | uniq -c | sort -rn | awk -v total="$REQUESTS" '
      { pct = 100 * $1 / total; bar = ""; for (i = 0; i < int(pct / 2); i++) bar = bar "#";
        printf "  %-52s %3d  %5.1f%%  %s\n", $2, $1, pct, bar; n++ }
      END { if (n == 0) print "  No X-Served-By header seen. Is the stack up and the endpoint reachable?" }'
}

distribution "3. Which instance answered?"

if [ "$KILL_ONE" = "yes" ]; then
  victim=$(docker compose ps -q "$SERVICE" | head -1)
  step "4. Stop one instance ($(docker inspect --format '{{.Name}}' "$victim" | sed 's#^/##'))"
  docker stop "$victim" >/dev/null
  echo "Waiting 8 s: Consul marks it failing and the gateway drops it from its list."
  sleep 8
  distribution "5. The same calls, with one instance gone"
  echo
  echo "Start it again with: docker compose up -d --scale ${SERVICE}=${INSTANCES}"
fi

echo
echo "Back to one instance: docker compose up -d --scale ${SERVICE}=1"
