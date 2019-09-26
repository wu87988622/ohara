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

container_name="ohara-demo-smb"

if [ ! "$(docker ps -q -f name=$container_name)" ]; then
  echo -e "\n> Start SMB server..."

  if [[ ! -d "$SMB_ROOT_DIR" ]]; then
    echo -e "create folder: $SMB_ROOT_DIR"
    mkdir -p $SMB_ROOT_DIR
  fi

  docker run -it --name $container_name --restart=always \
    -p 139:139 -p 445:445 \
    -v $SMB_ROOT_DIR:/mount -d dperson/samba \
    -u "$SMB_USER;$SMB_PASSWORD" \
    -s "$SMB_USER;/mount/;yes;no;no;all;$SMB_USER;$SMB_USER"

  # generate some data into a file
  temp_file=$(mktemp).csv
  echo "Id,Name,Created At" >> $temp_file
  for i in $(seq 1000);
    do
      randomString=$(head /dev/urandom | tr -dc a-z | head -c 10 ; echo '')
      createAt=$(date +"%Y-%m-%d %H:%M:%S" --date="+$i second")
      echo "$i,$randomString,$createAt" >> $temp_file
    done

  # move the file to SMB root folder
  mv -f $temp_file $SMB_ROOT_DIR/demo.csv
fi
