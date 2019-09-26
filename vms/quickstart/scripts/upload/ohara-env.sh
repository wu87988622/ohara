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

HOST=`ip -br addr | grep -E -o "192\.168\.[0-9]{1,3}\.[0-9]{1,3}" | head -n 1`
USER="ohara"
PASSWORD="oharastream"

CONFIGURATOR_HOST=$HOST
CONFIGURATOR_PORT=12345
CONFIGURATOR_FOLDER="/home/ohara/configurator"
CONFIGURATOR_API="http://$CONFIGURATOR_HOST:$CONFIGURATOR_PORT/v0"

MANAGER_HOST=$HOST
MANAGER_PORT=5050

FTP_HOST=$HOST
FTP_PORT=21
FTP_USER=$USER
FTP_PASSWORD=$PASSWORD

SMB_HOST=$HOST
SMB_PORT=445
SMB_USER=$USER
SMB_PASSWORD=$PASSWORD
SMB_ROOT_DIR="/home/ohara/smb"

PGSQL_HOST=$HOST
PGSQL_PORT=5432
PGSQL_USER=$USER
PGSQL_PASSWORD=$PASSWORD
PGSQL_DATABASE="postgres"
