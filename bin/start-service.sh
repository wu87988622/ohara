#!/bin/bash
#----------[LOCATE PROJECT]----------#
STARTTIME=$(date +'%s')
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

if [ "$1" == "configurator" ]; then
  CLASS="com.island.ohara.configurator.Configurator"
  shift 1
elif [ "$1" == "backend" ]; then
  CLASS="com.island.ohara.demo.Backend"
  shift 1
else
  echo "Usage: <configurator> [<args>]"
  exit 1
fi
ARGS=""
i=0
while [ -n "$1" ]
do
  ARGS=$ARGS" "$1
  i=$(($i+1))
  shift
done

#----------[EXECUTION]----------#
"$BIN_DIR/run_java.sh" $CLASS $ARGS