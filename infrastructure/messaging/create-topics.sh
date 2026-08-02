#!/usr/bin/env bash
set -Eeuo pipefail

bootstrap_server="kafka:9092"
kafka_topics="/opt/kafka/bin/kafka-topics.sh"

for attempt in $(seq 1 30); do
  if "$kafka_topics" --bootstrap-server "$bootstrap_server" --list >/dev/null 2>&1; then
    break
  fi

  if [[ "$attempt" -eq 30 ]]; then
    echo "Kafka did not become available after 30 attempts" >&2
    exit 1
  fi

  sleep 2
done

create_topic() {
  local topic_name="$1"
  local partitions="$2"
  local retention_ms="$3"
  local cleanup_policy="$4"

  "$kafka_topics" \
    --bootstrap-server "$bootstrap_server" \
    --create \
    --if-not-exists \
    --topic "$topic_name" \
    --partitions "$partitions" \
    --replication-factor 1 \
    --config "cleanup.policy=${cleanup_policy}" \
    --config "retention.ms=${retention_ms}" \
    --config "min.insync.replicas=1"
}

seven_days_ms=604800000
thirty_days_ms=2592000000
ninety_days_ms=7776000000

create_topic "commerce.order.v1" 6 "$seven_days_ms" "delete"
create_topic "commerce.inventory.v1" 6 "$seven_days_ms" "delete"
create_topic "commerce.payment.v1" 6 "$thirty_days_ms" "delete"
create_topic "commerce.delivery.v1" 6 "$thirty_days_ms" "delete"
create_topic "commerce.product.v1" 6 "$seven_days_ms" "compact,delete"

create_topic "commerce.order.v1.dlt" 3 "$ninety_days_ms" "delete"
create_topic "commerce.inventory.v1.dlt" 3 "$ninety_days_ms" "delete"
create_topic "commerce.payment.v1.dlt" 3 "$ninety_days_ms" "delete"
create_topic "commerce.delivery.v1.dlt" 3 "$ninety_days_ms" "delete"
create_topic "commerce.product.v1.dlt" 3 "$ninety_days_ms" "delete"

echo "Kafka topics and dead-letter topics are ready."
