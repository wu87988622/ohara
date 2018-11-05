#!/usr/bin/env bash
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
  # setup dependencies. TODO: remove this if OHARA-590 is resolved
  yarn setup:prod
  exec yarn start:prod $ARGS
else
  if [ "$service" == "configurator" ]; then
    CLASS="com.island.ohara.configurator.Configurator"
    shift 1
  elif [ "$service" == "backend" ]; then
    CLASS="com.island.ohara.demo.Backend"
    shift 1
  elif [ "$service" == "-v" ] || [ "$service" == "version" ] || [ "$service" == "-version" ]; then
    CLASS="com.island.ohara.util.VersionUtil"
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