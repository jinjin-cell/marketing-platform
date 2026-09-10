#!/usr/bin/env sh
set -eu

elasticsearch_url="${ELASTICSEARCH_URL:-http://127.0.0.1:9200}"
elasticsearch_user="${ELASTICSEARCH_USER:-qijiv}"
elasticsearch_password="${ELASTICSEARCH_PASSWORD:-123456}"
curl_auth="${elasticsearch_user}:${elasticsearch_password}"

create_index() {
  index_name="$1"
  mapping_file="$2"

  status="$(curl -sS -u "$curl_auth" -o /dev/null -w '%{http_code}' "${elasticsearch_url}/${index_name}")"
  if [ "$status" = "200" ]; then
    echo "Index ${index_name} already exists"
    return
  fi

  curl -fsS -u "$curl_auth" -X PUT "${elasticsearch_url}/${index_name}" \
    -H 'Content-Type: application/json' \
    --data-binary "@${mapping_file}"
  echo
}

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
create_index "big_market.raffle_activity_order" "${script_dir}/raffle-activity-order.json"
create_index "big_market.user_raffle_order" "${script_dir}/user-raffle-order.json"
