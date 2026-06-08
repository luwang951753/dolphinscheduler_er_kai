#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:12349}"
COOKIE_FILE="${COOKIE_FILE:-/tmp/dataflow-cookie.txt}"
USER_NAME="${USER_NAME:-admin}"
USER_PASSWORD="${USER_PASSWORD:-dolphinscheduler123}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-case_workbench}"

if [[ -z "${MYSQL_PASSWORD:-}" ]]; then
  echo "MYSQL_PASSWORD must be provided via environment variable." >&2
  exit 2
fi

curl_json() {
  curl -sS -m 15 -b "$COOKIE_FILE" "$@"
}

login() {
  curl -sS -m 15 -c "$COOKIE_FILE" -X POST "$BASE_URL/login" \
    --data-urlencode "userName=$USER_NAME" \
    --data-urlencode "userPassword=$USER_PASSWORD" >/dev/null
}

assert_success() {
  python3 - "$1" "$2" <<'PY'
import json
import sys

label, raw = sys.argv[1], sys.argv[2]
payload = json.loads(raw)
if not payload.get("success"):
    raise SystemExit(f"{label} failed: {raw}")
print(f"{label}: OK")
PY
}

assert_failure() {
  python3 - "$1" "$2" <<'PY'
import json
import sys

label, raw = sys.argv[1], sys.argv[2]
payload = json.loads(raw)
if payload.get("success"):
    raise SystemExit(f"{label} unexpectedly succeeded: {raw}")
print(f"{label}: expected failure OK")
PY
}

extract_id() {
  python3 - "$1" <<'PY'
import json
import sys

print(json.loads(sys.argv[1])["data"]["id"])
PY
}

name="qa_mysql_$(date +%s)"
payload=$(cat <<JSON
{"type":"MYSQL","name":"$name","note":"DataFlow QA 临时 MySQL 数据源","host":"$MYSQL_HOST","port":$MYSQL_PORT,"userName":"$MYSQL_USER","password":"$MYSQL_PASSWORD","database":"$MYSQL_DATABASE","connectType":"","other":{"serverTimezone":"Asia/Shanghai","useUnicode":"true","characterEncoding":"utf8"}}
JSON
)
bad_payload=$(cat <<JSON
{"type":"MYSQL","name":"${name}_bad","note":"DataFlow QA 错误密码验证","host":"$MYSQL_HOST","port":$MYSQL_PORT,"userName":"$MYSQL_USER","password":"wrong-password","database":"$MYSQL_DATABASE","connectType":"","other":{"serverTimezone":"Asia/Shanghai"}}
JSON
)

login

response=$(curl_json -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/connect" --data "$payload")
assert_success "connect valid MySQL datasource" "$response"

response=$(curl_json -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/connect" --data "$bad_payload")
assert_failure "connect invalid MySQL password" "$response"

response=$(curl_json -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources" --data "$payload")
assert_success "create datasource" "$response"
id=$(extract_id "$response")

cleanup() {
  curl_json -X DELETE "$BASE_URL/datasources/$id" >/dev/null || true
}
trap cleanup EXIT

response=$(curl_json "$BASE_URL/datasources/$id")
assert_success "query datasource" "$response"

response=$(curl_json "$BASE_URL/datasources/$id/connect-test")
assert_success "connection-test datasource" "$response"

update_payload=$(cat <<JSON
{"type":"MYSQL","name":"${name}_edited","note":"DataFlow QA 临时 MySQL 数据源-已编辑","host":"$MYSQL_HOST","port":$MYSQL_PORT,"userName":"$MYSQL_USER","password":"","database":"$MYSQL_DATABASE","connectType":"","other":{"serverTimezone":"Asia/Shanghai","useUnicode":"true","characterEncoding":"utf8"}}
JSON
)
response=$(curl_json -H 'Content-Type: application/json;charset=UTF-8' -X PUT "$BASE_URL/datasources/$id" --data "$update_payload")
assert_success "update datasource with empty password" "$response"

response=$(curl_json "$BASE_URL/datasources/$id/connect-test")
assert_success "connection-test datasource after update" "$response"

cleanup
trap - EXIT

response=$(curl_json "$BASE_URL/datasources/$id")
assert_failure "query deleted datasource" "$response"

echo "DataFlow datasource smoke passed."
