#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:12349}"
COOKIE_FILE="${COOKIE_FILE:-/tmp/dataflow-cookie.txt}"
USER_NAME="${USER_NAME:-admin}"
USER_PASSWORD="${USER_PASSWORD:-dolphinscheduler123}"
DATASOURCE_ID="${DATASOURCE_ID:-1}"
DATABASE="${DATABASE:-case_workbench}"
TABLE_NAME="${TABLE_NAME:-ajxx_tab}"

login() {
  curl -sS -m 15 -c "$COOKIE_FILE" -X POST "$BASE_URL/login" \
    --data-urlencode "userName=$USER_NAME" \
    --data-urlencode "userPassword=$USER_PASSWORD" >/dev/null
}

request() {
  curl -sS -m 20 -b "$COOKIE_FILE" "$@"
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

login

response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-data" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"tableName\":\"$TABLE_NAME\",\"filters\":[{\"field\":\"amount\",\"operator\":\"GT\",\"value\":\"500\"}],\"sorts\":[{\"field\":\"amount\",\"direction\":\"DESC\"}],\"pageNo\":1,\"pageSize\":3}")
assert_success "preview table data with filter and sort" "$response"

response=$(request "$BASE_URL/datasources/preview-table-structure?datasourceId=$DATASOURCE_ID&database=$DATABASE&tableName=$TABLE_NAME")
assert_success "preview table structure" "$response"

response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-sql" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"sql\":\"select case_id, case_no, amount from $TABLE_NAME order by amount desc\",\"pageSize\":2,\"timeoutSeconds\":5}")
assert_success "execute selected SQL" "$response"

response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-sql" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"sql\":\"select count(*) as total_count from $TABLE_NAME; select case_type, count(*) as cnt from $TABLE_NAME group by case_type order by cnt desc\",\"pageSize\":5,\"timeoutSeconds\":5,\"executeAll\":true}")
assert_success "execute all readonly SQL statements" "$response"

response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-sql-explain" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"sql\":\"select * from $TABLE_NAME where amount > 500\",\"pageSize\":10,\"timeoutSeconds\":5}")
assert_success "explain SQL" "$response"

response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-sql" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"sql\":\"delete from $TABLE_NAME where 1=0\",\"pageSize\":2,\"timeoutSeconds\":5}")
assert_failure "block write SQL" "$response"

view_name="qa_view_$(date +%s)"
view_config='{"filters":[{"field":"amount","operator":"GT","value":"500"}],"sorts":[{"field":"amount","direction":"DESC"}]}'
response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X POST "$BASE_URL/datasources/preview-views" --data "{\"datasourceId\":$DATASOURCE_ID,\"database\":\"$DATABASE\",\"tableName\":\"$TABLE_NAME\",\"viewName\":\"$view_name\",\"viewConfig\":$(python3 -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$view_config")}")
assert_success "create preview view" "$response"
view_id=$(extract_id "$response")

cleanup() {
  request -X DELETE "$BASE_URL/datasources/preview-views/$view_id" >/dev/null || true
}
trap cleanup EXIT

response=$(request "$BASE_URL/datasources/preview-views?datasourceId=$DATASOURCE_ID&database=$DATABASE&tableName=$TABLE_NAME")
assert_success "query preview views" "$response"

updated_config='{"columns":["case_id","case_no"]}'
response=$(request -H 'Content-Type: application/json;charset=UTF-8' -X PUT "$BASE_URL/datasources/preview-views/$view_id" --data "{\"viewName\":\"${view_name}_edited\",\"viewConfig\":$(python3 -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$updated_config")}")
assert_success "update preview view" "$response"

cleanup
trap - EXIT

response=$(request "$BASE_URL/datasources/preview-views?datasourceId=$DATASOURCE_ID&database=$DATABASE&tableName=$TABLE_NAME")
assert_success "query preview views after delete" "$response"

echo "DataFlow data preview smoke passed."
