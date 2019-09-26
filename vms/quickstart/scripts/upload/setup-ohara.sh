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

source ./ohara-env.sh

echo "HOST=$HOST"

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
  echo ""
  echo "Download completed!"
else
  echo "Ohara docker images already downloaded."
fi

# Remove all exited or dead containers
if [ "$(docker ps -q -f status=exited)" ]; then
  echo -e "\n> Remove all exited containers..."
  docker rm -f $(docker ps -qa --filter status=exited)
fi

if [ "$(docker ps -q -f status=dead)" ]; then
  echo -e "\n> Remove all dead containers..."
  docker rm -f $(docker ps -qa --filter status=dead)
fi

# Start Ohara services
./ohara-configurator.sh
./ohara-manager.sh
./ohara-demo-ftp.sh
./ohara-demo-postgresql.sh
./ohara-demo-smb.sh

sleep 3

if [ "$(docker ps -q -f name=ohara-demo-ftp)" ]; then
  echo -e "\n> FTP ready on ftp://$FTP_USER:$FTP_PASSWORD@$FTP_HOST:$FTP_PORT"
fi

if [ "$(docker ps -q -f name=ohara-demo-postgresql)" ]; then
  echo -e "\n> Postgresql ready on jdbc:postgresql://$PGSQL_HOST:$PGSQL_PORT/$PGSQL_DATABASE (user=$PGSQL_USER, password=$PGSQL_PASSWORD)"
fi

if [ "$(docker ps -q -f name=ohara-demo-smb)" ]; then
  echo -e "\n> SMB ready on smb://$SMB_USER:$SMB_PASSWORD@$SMB_HOST:$SMB_PORT/$SMB_USER"
fi

if [ "$(docker ps -q -f name=ohara-configurator)" ] && [ "$(docker ps -q -f name=ohara-manager)" ]; then
  echo -e "\n> Ohara ready on http://$MANAGER_HOST:$MANAGER_PORT \n"
else
  echo -e "\nStartup ohara was failure."
fi


