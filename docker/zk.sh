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
  if [[ -f "$ZOOKEEPER_HOME/bin/zookeeper_version" ]]; then
    echo "zookeeper $(cat "$ZOOKEEPER_HOME/bin/zookeeper_version")"
  else
    echo "zookeeper: unknown"
  fi
    if [[ -f "$ZOOKEEPER_HOME/bin/ohara_version" ]]; then
    echo "ohara $(cat "$ZOOKEEPER_HOME/bin/ohara_version")"
  else
    echo "ohara: unknown"
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

if [[ -z "$clientPort" ]]; then
  clientPort=2181
fi
echo "clientPort=$clientPort" >> "$CONFIG"

if [[ -z "$dataDir" ]]; then
  dataDir="/tmp/zookeeper/data"
fi
echo "dataDir=$dataDir" >> "$CONFIG"
mkdir -p $dataDir

if [[ -z "$peerPort" ]]; then
  peerPort=2888
fi

if [[ -z "$electionPort" ]]; then
  electionPort=3888
fi

if [[ -n "$servers" ]]; then
  serverIndex=0
  for server in $servers; do
    echo "server.$serverIndex=$server:$peerPort:$electionPort" >> "$CONFIG"
    serverIndex=$((serverIndex+1))
  done
fi

if [[ -z "$zkId" ]]; then
  zkId=0
fi
echo "$zkId" > "$dataDir/myid"

exec $ZOOKEEPER_HOME/bin/zkServer.sh start-foreground $ZOOKEEPER_HOME/conf/zoo.cfg
