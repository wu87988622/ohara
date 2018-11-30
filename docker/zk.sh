#!/bin/bash

if [[ -z "$ZOOKEEPER_HOME" ]];then
  echo "$ZOOKEEPER_HOME is required!!!"
  exit 2
fi

CONFIG=$ZOOKEEPER_HOME/conf/zoo.cfg
if [[ ! -f "$CONFIG" ]]; then
  # default setting
  echo "tickTime=2000" >> "$CONFIG"
  echo "initLimit=10" >> "$CONFIG"
  echo "syncLimit=5" >> "$CONFIG"
  echo "maxClientCnxns=60" >> "$CONFIG"

  if [[ -z "$ZK_PORT" ]]; then
    ZK_PORT=2181
  fi
  echo "clientPort=$ZK_PORT" >> "$CONFIG"

  if [[ -z "$ZK_DATA_DIR" ]]; then
    ZK_DATA_DIR="/tmp/zookeeper/data"
  fi
  echo "dataDir=$ZK_DATA_DIR" >> "$CONFIG"
  mkdir -p $ZK_DATA_DIR

  if [[ -n "$ZK_SERVERS" ]]; then
    for server in $ZK_SERVERS; do
      echo "$server" >> "$CONFIG"
    done
  fi
fi

if [[ -z "$ZK_ID" ]]; then
  ZK_ID=0
fi
echo "$ZK_ID" > "$ZK_DATA_DIR/myid"

exec $ZOOKEEPER_HOME/bin/zkServer.sh start-foreground $ZOOKEEPER_HOME/conf/zoo.cfg
