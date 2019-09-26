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

container_name="ohara-demo-ftp"

if [ ! "$(docker ps -q -f name=$container_name)" ]; then
  echo -e "\n> Start FTP server..."
  docker run -d -v ftp_data:/home/vsftpd --name $container_name \
    -p 20:20 -p $FTP_PORT:$FTP_PORT -p 21100-21110:21100-21110 \
    -e FTP_USER=$FTP_USER -e FTP_PASS=$FTP_PASSWORD \
    -e PASV_ADDRESS=$FTP_HOST -e PASV_MIN_PORT=21100 -e PASV_MAX_PORT=21110 \
    --restart=always fauria/vsftpd

  # generate some data into a file and upload to FTP server
  temp_file=$(mktemp).csv
  echo "Id,Name,Created At" >> $temp_file
  for i in $(seq 1000);
    do
      randomString=$(head /dev/urandom | tr -dc a-z | head -c 10 ; echo '')
      createAt=$(date +"%Y-%m-%d %H:%M:%S" --date="+$i second")
      echo "$i,$randomString,$createAt" >> $temp_file
    done

  # upload to FTP
  curl -T $temp_file ftp://$FTP_HOST/demo.csv --user $FTP_USER:$FTP_PASSWORD
  rm -rf $temp_file
fi
