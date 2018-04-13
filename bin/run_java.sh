#!/bin/bash
#----------[LOCATE PROJECT]----------#
date
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

#----------[IPV4]----------#
IPV4_PREFER="-Djava.net.preferIPv4Stack=true"

#----------[HEAP SIZE]----------#
HEAPSIZE="-Xmx4000m"

#----------[JAVA]----------#
JAVA="java $HEAPSIZE -cp"

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
$JAVA $CLASSPATH $LOG4J $CLASS $ARGS

date
ENDTIME=$(date +%s)
echo "It takes $(($ENDTIME - $STARTTIME)) seconds to complete this task..."