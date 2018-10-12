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

# our manager is running background so a way of breaking manager is necessary
if [ "$1" == "manager" ]; then
  cd "$PROJECT_HOME/manager"
  yarn clean:process
  exit
elif [ "$1" == "help" ]; then
  echo "Usage:"
  echo "Option                                   Description"
  echo "--------                                 -----------"
  echo "manager                                  Stop the Ohara Manager."
else
  echo "Usage: (manager|help)"
  exit 1
fi