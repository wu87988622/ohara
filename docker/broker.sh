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
  if [[ -f "$KAFKA_HOME/bin/broker_version" ]]; then
    echo "broker $(cat "$KAFKA_HOME/bin/broker_version")"
  else
    echo "broker: unknown"
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

CONFIG=$KAFKA_HOME/config/broker.config
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# jmx setting

if [[ -z $jmxPort ]]; then
  jmxPort="9093"
fi

if [[ -z $jmxHostname ]]; then
  echo "jmxHostname is required!!!"
  exit 2
fi

# this option will rewrite the default setting in kafka script
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.port=$jmxPort \
-Dcom.sun.management.jmxremote.rmi.port=$jmxPort \
-Djava.rmi.server.hostname=$jmxHostname
"

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
if [[ -z "${brokerId}" ]]; then
  brokerId="0"
fi
echo "broker.id=$brokerId" >> "$CONFIG"

if [[ -z "${clientPort}" ]]; then
  clientPort=9092
fi
echo "listeners=PLAINTEXT://:$clientPort" >> "$CONFIG"

if [[ -z "${dataDir}" ]]; then
  dataDir="/tmp/broker/data"
fi
echo "log.dirs=$dataDir" >> "$CONFIG"

if [[ -z "${zookeepers}" ]]; then
  echo "You have to define zookeepers"
  exit 2
fi
echo "zookeeper.connect=$zookeepers" >> "$CONFIG"

if [[ -z "$advertisedClientPort" ]]; then
  advertisedClientPort=$clientPort
fi

if [[ -n "$advertisedHostname" ]]; then
  echo "advertised.listeners=PLAINTEXT://$advertisedHostname:$advertisedClientPort" >> "$CONFIG"
fi

if [[ -z "$KAFKA_HOME" ]]; then
  echo "KAFKA_HOME is required!!!"
  exit 2
fi

if [[ ! -z "$PROMETHEUS_EXPORTER" ]]; then
  if [[ ! -f "$PROMETHEUS_EXPORTER" ]]; then
    echo "PROMETHEUS_EXPORTER exporter doesn't exist!!!"
    exit 2
  fi

  if [[ ! -f "$PROMETHEUS_EXPORTER_CONFIG" ]]; then
    echo "PROMETHEUS_EXPORTER_CONFIG exporter config doesn't exist!!!"
    exit 2
  fi

  if [[ -z "$exporterPort" ]]; then
    exporterPort="7071"
  fi

  export KAFKA_OPTS="-javaagent:$PROMETHEUS_EXPORTER=$exporterPort:$PROMETHEUS_EXPORTER_CONFIG"
fi

exec $KAFKA_HOME/bin/kafka-server-start.sh "$CONFIG"
