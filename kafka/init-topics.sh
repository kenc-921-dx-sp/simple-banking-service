#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="kafka:29092"

echo "Creating Kafka topics..."

kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create \
  --if-not-exists \
  --topic account-transactions \
  --partitions 6 \
  --replication-factor 1

kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create \
  --if-not-exists \
  --topic account-transactions-dlq \
  --partitions 3 \
  --replication-factor 1

kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --create \
  --if-not-exists \
  --topic exchange-rate-events \
  --partitions 3 \
  --replication-factor 1

echo "Kafka topics created:"
kafka-topics.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --list