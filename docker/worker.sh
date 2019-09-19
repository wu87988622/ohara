#!/usr/bin/env bash
#
# Copyright 2019 is-land
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


if [[ "$1" == "-v" ]] || [[ "$1" == "-version" ]]; then
  if [[ -f "$KAFKA_HOME/bin/worker_version" ]]; then
    echo "connect-worker $(cat "$KAFKA_HOME/bin/worker_version")"
  else
    echo "connect-worker: unknown"
  fi
    if [[ -f "$KAFKA_HOME/bin/ohara_version" ]]; then
    echo "ohara $(cat "$KAFKA_HOME/bin/ohara_version")"
  else
    echo "ohara: unknown"
  fi
  exit
fi

if [[ -z "$KAFKA_HOME" ]];then
  echo "$KAFKA_HOME is required!!!"
  exit 2
fi

CONFIG=$KAFKA_HOME/config/worker.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# jmx setting

if [[ -z $jmxPort ]]; then
  $jmxPort="8084"
fi

if [[ -z $JMX_HOSTNAME ]]; then
  echo "JMX_HOSTNAME is required!!!"
  exit 2
fi

# this option will rewrite the default setting in kafka script
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.port=$jmxPort \
-Dcom.sun.management.jmxremote.rmi.port=$jmxPort \
-Djava.rmi.server.hostname=$JMX_HOSTNAME
"

# We need to change working folder to another one which contains less files. Kafka had an issue
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

if [[ -z "$groupId" ]]; then
  groupId="connect-cluster"
fi
echo "group.id=$groupId" >> "$CONFIG"

if [[ -z "$offsetTopicName" ]]; then
  offsetTopicName="connect-offsets"
fi
if [[ -z "$offsetTopicReplications" ]]; then
  offsetTopicReplications="1"
fi
if [[ -z "$offsetTopicPartitions" ]]; then
  offsetTopicPartitions="1"
fi
echo "offset.storage.topic=$offsetTopicName" >> "$CONFIG"
echo "offset.storage.replication.factor=$offsetTopicReplications" >> "$CONFIG"
echo "offset.storage.partitions=$offsetTopicPartitions" >> "$CONFIG"

if [[ -z "$configTopicName" ]]; then
  configTopicName="connect-config"
fi
if [[ -z "$configTopicReplications" ]]; then
  configTopicReplications="1"
fi
echo "config.storage.topic=$configTopicName" >> "$CONFIG"
echo "config.storage.replication.factor=$configTopicReplications" >> "$CONFIG"
# config topic should be a single partition

if [[ -z "$statusTopicName" ]]; then
  statusTopicName="connect-offsets"
fi
if [[ -z "$statusTopicReplications" ]]; then
  statusTopicReplications="1"
fi
if [[ -z "$statusTopicPartitions" ]]; then
  statusTopicPartitions="1"
fi
echo "status.storage.topic=$statusTopicName" >> "$CONFIG"
echo "status.storage.replication.factor=$statusTopicReplications" >> "$CONFIG"
echo "status.storage.partitions=$statusTopicPartitions" >> "$CONFIG"
WORKER_PLUGIN_FOLDER="/tmp/plugins"
mkdir -p $WORKER_PLUGIN_FOLDER

if [[ ! -z "$WORKER_JAR_URLS" ]] && [[ -z "$jarInfos" ]]; then
  echo "jarInfos is required since you have set the WORKER_JAR_URLS:$WORKER_JAR_URLS"
  exit 2
fi

if [[ -z "$WORKER_JAR_URLS" ]] && [[ ! -z "$jarInfos" ]]; then
  echo "WORKER_JAR_URLS is required since you have set the jarInfos:$jarInfos"
  exit 2
fi

if [[ ! -z "$WORKER_JAR_URLS" ]]; then
  IFS=','
  read -ra ADDR <<< "$WORKER_JAR_URLS"
  for i in "${ADDR[@]}"; do
    wget $i -P $KAFKA_HOME/libs
  done
fi

if [[ -z "$WORKER_BROKERS" ]]; then
  echo "You have to define WORKER_BROKERS"
  exit 2
fi
echo "bootstrap.servers=$WORKER_BROKERS" >> "$CONFIG"

if [[ -z "$clientPort" ]]; then
  clientPort="8083"
fi
echo "rest.port=$clientPort" >> "$CONFIG"

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