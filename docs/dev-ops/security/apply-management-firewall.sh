#!/usr/bin/env bash

set -euo pipefail

: "${MANAGEMENT_CIDRS:?MANAGEMENT_CIDRS must contain one or more trusted IPv4 CIDRs}"

readonly PUBLIC_INTERFACE="${PUBLIC_INTERFACE:-eth0}"
readonly FILTER_CHAIN="MP-MANAGEMENT"
readonly MANAGEMENT_PORTS=(
  2181   # ZooKeeper
  5672   # RabbitMQ AMQP
  8848   # Nacos HTTP
  9000   # Portainer
  9090   # Jenkins
  9091   # XXL-Job admin
  9848   # Nacos gRPC
  13306  # MySQL published port
  15672  # RabbitMQ management
  16379  # Redis published port
  50001  # Jenkins agent
)

if [[ "${EUID}" -ne 0 ]]; then
  echo "This script must run as root." >&2
  exit 1
fi

if ! iptables -w 10 -S DOCKER-USER >/dev/null 2>&1; then
  echo "The DOCKER-USER chain is unavailable; start Docker before applying this policy." >&2
  exit 1
fi

iptables -w 10 -N "${FILTER_CHAIN}" 2>/dev/null || true
iptables -w 10 -F "${FILTER_CHAIN}"

read -r -a trusted_cidrs <<< "${MANAGEMENT_CIDRS//,/ }"
for cidr in "${trusted_cidrs[@]}"; do
  [[ -n "${cidr}" ]] || continue
  iptables -w 10 -A "${FILTER_CHAIN}" -s "${cidr}" -j RETURN
done

if [[ "$(iptables -w 10 -S "${FILTER_CHAIN}" | wc -l)" -le 1 ]]; then
  echo "No valid management CIDR was supplied." >&2
  exit 1
fi

iptables -w 10 -A "${FILTER_CHAIN}" -j DROP

for port in "${MANAGEMENT_PORTS[@]}"; do
  rule=(
    -i "${PUBLIC_INTERFACE}"
    -p tcp
    -m conntrack
    --ctdir ORIGINAL
    --ctorigdstport "${port}"
    -j "${FILTER_CHAIN}"
  )
  if ! iptables -w 10 -C DOCKER-USER "${rule[@]}" 2>/dev/null; then
    iptables -w 10 -I DOCKER-USER 1 "${rule[@]}"
  fi
done

echo "Management-port policy applied for: ${MANAGEMENT_CIDRS}"
