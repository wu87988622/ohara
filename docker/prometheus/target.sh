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

if [[ -z "${PROMETHEUS_HOME}" ]]; then
  echo "PROMETHEUS_HOME needs"
  exit 2
fi

if [[ -z "${PROMETHEUS_PORT}" ]]; then
  PROMETHEUS_PORT=9090
fi

PROMETHEUS_CONFIG="${PROMETHEUS_HOME}/config"
if [ -z "$(ls -A "$PROMETHEUS_CONFIG"/targets)" ]; then
  cp $PROMETHEUS_CONFIG/targets.json $PROMETHEUS_CONFIG/targets
  TARGET=""

# target env
  if [[ ! -z "${PROMETHEUS_TARGETS}" ]]; then
    echo "MyPrometheus has targets : $PROMETHEUS_TARGETS"
    TARGET="$PROMETHEUS_TARGETS"
  fi

# monitor self
  if [ "YES" = "${PROMETHEUS_LOCAL}" ]; then
    if [[ ! -z "${TARGET}" ]]; then
      TARGET="127.0.0.1:$PROMETHEUS_PORT,$TARGET"
    else
      TARGET="127.0.0.1:$PROMETHEUS_PORT"
    fi
  fi

# set targets
  TARGETS="\"\""
  IFS=',' read -r -a array <<< $TARGET
  for element in "${array[@]}"
  do
    if [[ "\"\"" = "${TARGETS}" ]]; then
      TARGETS="\"${element}\""
    else
      TARGETS="${TARGETS},\"${element}\""
    fi
  done
  echo $(jq ".[].targets=[$TARGETS]" $PROMETHEUS_CONFIG/targets/targets.json) > $PROMETHEUS_CONFIG/targets/targets.json

fi

exec $PROMETHEUS_HOME/prometheus --config.file=$PROMETHEUS_HOME/config/prometheus.yml --web.listen-address=:$PROMETHEUS_PORT
