#!/usr/bin/env bash

# this script is used to start the worker.
# it accept the env variables and then generate the configuration file used in starting broker or worker.

CONFIG=$HOME/server.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# default setting
echo "key.converter=org.apache.kafka.connect.json.JsonConverter" >> "$CONFIG"
echo "value.converter=org.apache.kafka.connect.json.JsonConverter" >> "$CONFIG"
echo "key.converter.schemas.enable=true" >> "$CONFIG"
echo "value.converter.schemas.enable=true" >> "$CONFIG"
echo "offset.flush.interval.ms=10000" >> "$CONFIG"

# those configs are required to kafka 1.x
echo "internal.key.converter=org.apache.kafka.connect.json.JsonConverter" >> "$CONFIG"
echo "internal.value.converter=org.apache.kafka.connect.json.JsonConverter" >> "$CONFIG"
echo "internal.key.converter.schemas.enable=false" >> "$CONFIG"
echo "internal.value.converter.schemas.enable=false" >> "$CONFIG"

if [[ -z "${WORKER_GROUP}" ]]; then
  WORKER_GROUP="connect-cluster"
fi
echo "group.id=$WORKER_GROUP" >> "$CONFIG"

if [[ -z "${WORKER_OFFSET_TOPIC}" ]]; then
  WORKER_OFFSET_TOPIC="connect-offsets"
fi
echo "offset.storage.topic=$WORKER_OFFSET_TOPIC" >> "$CONFIG"
echo "offset.storage.replication.factor=1" >> "$CONFIG"

if [[ -z "${WORKER_CONFIG_TOPIC}" ]]; then
  WORKER_CONFIG_TOPIC="connect-config"
fi
echo "config.storage.topic=$WORKER_CONFIG_TOPIC" >> "$CONFIG"
echo "config.storage.replication.factor=1" >> "$CONFIG"

if [[ -z "${WORKER_STATUS_TOPIC}" ]]; then
  WORKER_STATUS_TOPIC="connect-offsets"
fi
echo "status.storage.topic=$WORKER_STATUS_TOPIC" >> "$CONFIG"
echo "status.storage.replication.factor=1" >> "$CONFIG"

if [[ -z "$WORKER_PLUGIN_FOLDER" ]]; then
  WORKER_PLUGIN_FOLDER="/tmp/plugins"
fi
echo "plugin.path=$WORKER_PLUGIN_FOLDER" >> "$CONFIG"

if [[ -z "$WORKER_BROKERS" ]]; then
  echo "You have to define BROKERS"
  exit 2
fi
echo "bootstrap.servers=$WORKER_BROKERS" >> "$CONFIG"

if [[ -z "${WORKER_CLIENT_PORT}" ]]; then
  WORKER_CLIENT_PORT="8083"
fi
echo "rest.port=$WORKER_CLIENT_PORT" >> "$CONFIG"

if [[ -n "$WORKER_HOSTNAME" ]]; then
  echo "rest.host.name=$WORKER_HOSTNAME" >> "$CONFIG"
fi

if [[ -n "$WORKER_ADVERTISED_HOSTNAME" ]]; then
  echo "rest.advertised.host.name=$WORKER_ADVERTISED_HOSTNAME" >> "$CONFIG"
fi

if [[ -n "$WORKER_ADVERTISED_PORT" ]]; then
  echo "rest.advertised.port=$WORKER_ADVERTISED_PORT" >> "$CONFIG"
fi

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi
exec $KAFKA_HOME/bin/connect-distributed.sh "$CONFIG"