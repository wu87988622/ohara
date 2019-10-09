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

# parsing arguments
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
    -v|--version)
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
    ;;
    -f|--file)
    FILE_DATA+=("$2")
    shift
    shift
    ;;
  esac
done

# parsing files
## we will loop all the files in FILE_DATA of arguments : --file A --file B --file C
## the format of A, B, C should be file_name=k1=v1,k2=v2,k3,k4=v4...
for f in "${FILE_DATA[@]}"; do
  key=${f%%=*}
  value=${f#*=}
  dir=$(dirname "$key")

  [ -d $dir ] || mkdir -p $dir

  IFS=',' read -ra VALUES <<< "$value"
  for i in "${VALUES[@]}"; do
    echo "-- save line : \"$i\" to $key"
    echo "$i" >> $key
  done
done

if [[ -z "$ZOOKEEPER_HOME" ]];then
  echo "$ZOOKEEPER_HOME is required!!!"
  exit 2
fi

exec $ZOOKEEPER_HOME/bin/zkServer.sh start-foreground $ZOOKEEPER_HOME/conf/zoo.cfg
