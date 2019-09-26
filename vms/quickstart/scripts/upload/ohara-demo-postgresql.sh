#!/usr/bin/env bash
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

container_name="ohara-demo-postgresql"

if [ ! "$(docker ps -q -f name=$container_name)" ]; then
  echo -e "\n> Start postgresql server..."

  docker volume create pg_data

  docker run --name $container_name --restart=always \
    -v pg_data:/var/lib/pgsql -p $PGSQL_PORT:$PGSQL_PORT \
    --env "POSTGRES_USER=$PGSQL_USER" \
    --env "POSTGRES_PASSWORD=$PGSQL_PASSWORD" \
    --env "POSTGRES_DB=$PGSQL_DATABASE" \
    -d islandsystems/postgresql:9.2.24

  sleep 3

  # create a table employees and insert some data
  docker cp ~/employees.sql $container_name:/tmp/employees.sql
  docker exec -u postgres $container_name psql -f /tmp/employees.sql
fi
