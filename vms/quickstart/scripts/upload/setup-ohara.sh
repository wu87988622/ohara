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


# variable `OHARA_VER` should pass from command
if [[ -z "$OHARA_VER" ]]; then
  echo "Cannot find environment variable: OHARA_VER"
  exit 1
fi

echo "OHARA_VER=$OHARA_VER"

# Just pull Ohara images once
OHARA_IMAGES=$(docker images --filter reference="oharastream/*" -q)
if [[ -z "$OHARA_IMAGES" ]]; then
  echo "Pull Ohara images..."
  docker pull "oharastream/configurator:$OHARA_VER"
  docker pull "oharastream/manager:$OHARA_VER"
  docker pull "oharastream/broker:$OHARA_VER"
  docker pull "oharastream/zookeeper:$OHARA_VER"
  docker pull "oharastream/connect-worker:$OHARA_VER"
  docker pull "oharastream/streamapp:$OHARA_VER"
  docker pull "oharastream/shabondi:$OHARA_VER"
  docker pull "oharastream/backend:$OHARA_VER"
  echo ""
  echo "Download completed!"
else
  echo "Ohara docker images already downloaded."
fi

echo "IP address info:"
ip -br addr
echo ""
echo "Now you can run:"
printf "  $ ./ohara-configurator.sh\n"
printf "  $ ./ohara-manager.sh {IP}\n"
