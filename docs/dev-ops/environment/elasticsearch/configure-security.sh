#!/usr/bin/env sh
set -eu

elasticsearch_url="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
bootstrap_user="${ELASTICSEARCH_BOOTSTRAP_USER:-elastic}"
bootstrap_password="${ELASTICSEARCH_BOOTSTRAP_PASSWORD:?ELASTICSEARCH_BOOTSTRAP_PASSWORD is required}"
project_user="${ELASTICSEARCH_PROJECT_USER:-qijiv}"
project_password="${ELASTICSEARCH_PROJECT_PASSWORD:?ELASTICSEARCH_PROJECT_PASSWORD is required}"
kibana_password="${KIBANA_SYSTEM_PASSWORD:?KIBANA_SYSTEM_PASSWORD is required}"

until curl -fsS -u "${bootstrap_user}:${bootstrap_password}" "${elasticsearch_url}/_cluster/health" >/dev/null; do
  sleep 2
done

curl -fsS -u "${bootstrap_user}:${bootstrap_password}" \
  -X POST "${elasticsearch_url}/_security/user/${project_user}" \
  -H 'Content-Type: application/json' \
  -d "{\"password\":\"${project_password}\",\"roles\":[\"superuser\"],\"full_name\":\"Marketing Platform\"}"
echo

curl -fsS -u "${bootstrap_user}:${bootstrap_password}" \
  -X POST "${elasticsearch_url}/_security/user/kibana_system/_password" \
  -H 'Content-Type: application/json' \
  -d "{\"password\":\"${kibana_password}\"}"
echo
