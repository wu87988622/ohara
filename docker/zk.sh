#!/bin/bash
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
  if [[ -f "$ZOOKEEPER_HOME/bin/true_version" ]]; then
    VERSION=$(cat "$ZOOKEEPER_HOME/bin/true_version")
    echo "zookeeper $VERSION"
  else
    echo "zookeeper: unknown"
  fi
  exit
fi

if [[ -z "$ZOOKEEPER_HOME" ]];then
  echo "$ZOOKEEPER_HOME is required!!!"
  exit 2
fi

CONFIG=$ZOOKEEPER_HOME/conf/zoo.cfg
if [[ -f "$CONFIG" ]]; then
  echo "$CONFIG already exists!!!"
  exit 2
fi

# default setting
echo "tickTime=2000" >> "$CONFIG"
echo "initLimit=10" >> "$CONFIG"
echo "syncLimit=5" >> "$CONFIG"
echo "maxClientCnxns=60" >> "$CONFIG"

if [[ -z "$ZK_CLIENT_PORT" ]]; then
  ZK_CLIENT_PORT=2181
fi
echo "clientPort=$ZK_CLIENT_PORT" >> "$CONFIG"

if [[ -z "$ZK_DATA_DIR" ]]; then
  ZK_DATA_DIR="/tmp/zookeeper/data"
fi
echo "dataDir=$ZK_DATA_DIR" >> "$CONFIG"
mkdir -p $ZK_DATA_DIR

if [[ -z "$ZK_PEER_PORT" ]]; then
  ZK_PEER_PORT=2888
fi

if [[ -z "$ZK_ELECTION_PORT" ]]; then
  ZK_ELECTION_PORT=3888
fi

if [[ -n "$ZK_SERVERS" ]]; then
  serverIndex=0
  for server in $ZK_SERVERS; do
    echo "server.$serverIndex=$server:$ZK_PEER_PORT:$ZK_ELECTION_PORT" >> "$CONFIG"
    serverIndex=$((serverIndex+1))
  done
fi

if [[ -z "$ZK_ID" ]]; then
  ZK_ID=0
fi
echo "$ZK_ID" > "$ZK_DATA_DIR/myid"

exec $ZOOKEEPER_HOME/bin/zkServer.sh start-foreground $ZOOKEEPER_HOME/conf/zoo.cfg
