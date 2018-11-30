#!/usr/bin/env bash

# this script is used to start the broker.
# it accept the env variables and then generate the configuration file used in starting broker or worker.

CONFIG=$HOME/server.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# default setting
echo "num.network.threads=3" >> "$CONFIG"
echo "num.io.threads=8" >> "$CONFIG"
echo "socket.send.buffer.bytes=102400" >> "$CONFIG"
echo "socket.receive.buffer.bytes=102400" >> "$CONFIG"
echo "socket.request.max.bytes=104857600" >> "$CONFIG"
echo "num.partitions=1" >> "$CONFIG"
echo "num.recovery.threads.per.data.dir=1" >> "$CONFIG"
echo "offsets.topic.replication.factor=1" >> "$CONFIG"
echo "transaction.state.log.replication.factor=1" >> "$CONFIG"
echo "transaction.state.log.min.isr=1" >> "$CONFIG"
echo "log.retention.hours=168" >> "$CONFIG"
echo "log.segment.bytes=1073741824" >> "$CONFIG"
echo "log.retention.check.interval.ms=300000" >> "$CONFIG"
echo "zookeeper.connection.timeout.ms=6000" >> "$CONFIG"
echo "group.initial.rebalance.delay.ms=0" >> "$CONFIG"
if [[ -z "${BROKER_ID}" ]]; then
  BROKER_ID="0"
fi
echo "broker.id=$BROKER_ID" >> "$CONFIG"

if [[ -z "${BROKER_ADDRESS}" ]]; then
  BROKER_ADDRESS="PLAINTEXT://:9092"
fi
echo "listeners=$BROKER_ADDRESS" >> "$CONFIG"

if [[ -z "${BROKER_DATA_DIR}" ]]; then
  BROKER_DATA_DIR="/tmp/broker/data"
fi
echo "log.dirs=$BROKER_DATA_DIR" >> "$CONFIG"

if [[ -z "${ZOOKEEPERS}" ]]; then
  echo "You have to define ZOOKEEPERS"
  exit 2
fi
echo "zookeeper.connect=$ZOOKEEPERS" >> "$CONFIG"

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi
exec $KAFKA_HOME/bin/kafka-server-start.sh "$CONFIG"
