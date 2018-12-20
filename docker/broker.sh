#!/usr/bin/env bash

if [[ -z "$KAFKA_HOME" ]];then
  echo "$KAFKA_HOME is required!!!"
  exit 2
fi

CONFIG=$KAFKA_HOME/config/broker.config
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

if [[ -z "${BROKER_CLIENT_PORT}" ]]; then
  BROKER_CLIENT_PORT=9092
fi
echo "listeners=PLAINTEXT://:$BROKER_CLIENT_PORT" >> "$CONFIG"

if [[ -z "${BROKER_DATA_DIR}" ]]; then
  BROKER_DATA_DIR="/tmp/broker/data"
fi
echo "log.dirs=$BROKER_DATA_DIR" >> "$CONFIG"

if [[ -z "${BROKER_ZOOKEEPERS}" ]]; then
  echo "You have to define BROKER_ZOOKEEPERS"
  exit 2
fi
echo "zookeeper.connect=$BROKER_ZOOKEEPERS" >> "$CONFIG"

if [[ -z "$BROKER_ADVERTISED_CLIENT_PORT" ]]; then
  BROKER_ADVERTISED_CLIENT_PORT=$BROKER_CLIENT_PORT
fi

if [[ -n "$BROKER_ADVERTISED_HOSTNAME" ]]; then
  echo "advertised.listeners=PLAINTEXT://$BROKER_ADVERTISED_HOSTNAME:$BROKER_ADVERTISED_CLIENT_PORT" >> "$CONFIG"
fi

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi
exec $KAFKA_HOME/bin/kafka-server-start.sh "$CONFIG"
