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

#----------[JAVA]----------#
if [ -z "$OHARA_OPTS" ]; then
  JAVA="java -cp"
else
  JAVA="java $OHARA_OPTS -cp"
fi

#----------[CLASSPATH]----------#
CLASSPATH="${PROJECT_HOME}/lib/*:${PROJECT_HOME}/conf/:"

#----------[LOG4J]----------#
LOG4J=-Dlog4j.configuration=file:$PROJECT_HOME'/conf/log4j.properties'

#----------[FIND CLASS]----------#
CLASS=''
if [ "$1" != "" ]; then
  CLASS=$1
  shift 1
else
  echo "Usage: run run_class <class> [<args>]"
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
exec $JAVA $CLASSPATH $LOG4J $CLASS $ARGS