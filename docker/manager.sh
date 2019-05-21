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

if [[ "$1" == "-v" ]] || [[ "$1" == "version" ]] || [[ "$1" == "-version" ]]; then
  if [[ -f "$OHARA_HOME/bin/ohara_version" ]]; then
    cat $OHARA_HOME/bin/ohara_version
  else
    echo "ohara manager: unknown"
  fi
  exit
fi

exec ohara.sh start manager "$@"