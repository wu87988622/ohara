#!/usr/bin/env bash

if [[ -z "$KAFKA_HOME" ]];then
  echo "$KAFKA_HOME is required!!!"
  exit 2
fi

CONFIG=$KAFKA_HOME/config/worker.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# We need to change working folder to another one which contains less files. Kafka had a issue
# (see https://issues.apache.org/jira/browse/KAFKA-4247) which generating classpath starting with colon and it make
# reflection tool scan all stuff under the current folder. It may be slow if the folder has many files...
# TODO: remove this workaround if we upgrade the kafka to 1.1.0 +
cd $KAFKA_HOME/bin

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
if [[ -z "${WORKER_OFFSET_TOPIC_REPLICATIONS}" ]]; then
  WORKER_OFFSET_TOPIC_REPLICATIONS="1"
fi
if [[ -z "${WORKER_OFFSET_TOPIC_PARTITIONS}" ]]; then
  WORKER_OFFSET_TOPIC_PARTITIONS="1"
fi
echo "offset.storage.topic=$WORKER_OFFSET_TOPIC" >> "$CONFIG"
echo "offset.storage.replication.factor=$WORKER_OFFSET_TOPIC_REPLICATIONS" >> "$CONFIG"
echo "offset.storage.partitions=$WORKER_OFFSET_TOPIC_PARTITIONS" >> "$CONFIG"

if [[ -z "${WORKER_CONFIG_TOPIC}" ]]; then
  WORKER_CONFIG_TOPIC="connect-config"
fi
if [[ -z "${WORKER_CONFIG_TOPIC_REPLICATIONS}" ]]; then
  WORKER_CONFIG_TOPIC_REPLICATIONS="1"
fi
echo "config.storage.topic=$WORKER_CONFIG_TOPIC" >> "$CONFIG"
echo "config.storage.replication.factor=$WORKER_CONFIG_TOPIC_REPLICATIONS" >> "$CONFIG"
# config topic should be a single partition

if [[ -z "${WORKER_STATUS_TOPIC}" ]]; then
  WORKER_STATUS_TOPIC="connect-offsets"
fi
if [[ -z "${WORKER_STATUS_TOPIC_REPLICATIONS}" ]]; then
  WORKER_STATUS_TOPIC_REPLICATIONS="1"
fi
if [[ -z "${WORKER_STATUS_TOPIC_PARTITIONS}" ]]; then
  WORKER_STATUS_TOPIC_PARTITIONS="1"
fi
echo "status.storage.topic=$WORKER_STATUS_TOPIC" >> "$CONFIG"
echo "status.storage.replication.factor=$WORKER_STATUS_TOPIC_REPLICATIONS" >> "$CONFIG"
echo "status.storage.partitions=$WORKER_STATUS_TOPIC_PARTITIONS" >> "$CONFIG"

if [[ -z "$WORKER_PLUGIN_FOLDER" ]]; then
  WORKER_PLUGIN_FOLDER="/tmp/plugins"
fi
echo "plugin.path=$WORKER_PLUGIN_FOLDER" >> "$CONFIG"

if [[ -z "$WORKER_BROKERS" ]]; then
  echo "You have to define WORKER_BROKERS"
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

if [[ -n "$WORKER_ADVERTISED_CLIENT_PORT" ]]; then
  echo "rest.advertised.port=$WORKER_ADVERTISED_CLIENT_PORT" >> "$CONFIG"
fi

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi
exec $KAFKA_HOME/bin/connect-distributed.sh "$CONFIG"