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

#----------[LOCATE PROJECT]----------#
SOURCE="${BASH_SOURCE[0]}"
BIN_DIR="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  BIN_DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
BIN_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
PROJECT_HOME="$(dirname "$BIN_DIR")"

service=$1
shift 1
ARGS=""
i=0
while [ -n "$1" ]
do
  ARGS=$ARGS" "$1
  i=$(($i+1))
  shift
done

if [ "$service" == "manager" ]; then
  cd "$PROJECT_HOME/manager"
  exec node ./index.js $ARGS
else
  if [ "$service" == "configurator" ]; then
    CLASS="com.island.ohara.configurator.Configurator"
    shift 1
  elif [ "$service" == "backend" ]; then
    CLASS="com.island.ohara.demo.Backend"
    shift 1
  elif [ "$service" == "-v" ] || [ "$service" == "version" ] || [ "$service" == "-version" ]; then
    CLASS="com.island.ohara.common.util.VersionUtil"
    shift 1
  elif [ "$service" == "help" ]; then
    echo "Usage:"
    echo "Option                                   Description"
    echo "-----------                              -----------"
    echo "configurator                             Ohara Configurator provides the service "
    echo "                                         for user and Ohara Manager to use."
    echo ""
    echo "backend                                  Used for a testing purpose. It doesn't work in production."
    echo ""
    echo "manager                                  Running Ohara Manager. After run this command, you can "
    echo "                                         connect to http://\${HostName or IP}:5050 url by browser."
    exit 1
  else
    echo "Usage: (configurator|backend|manager|help) [<args>]"
    exit 1
  fi
  #----------[EXECUTION]----------#
  exec "$BIN_DIR/run_java.sh" $CLASS $ARGS
fi
