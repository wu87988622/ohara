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

# set nginx proxy to configurator

# copy basic config
cp /etc/nginx/default.conf /etc/nginx/conf.d/default.conf
echo "proxy url :${CONFIGURATORURL}"
if [[ -z "${CONFIGURATORURL}" ]]; then
  sed -i -e 's@{proxyStting}@@g' /etc/nginx/conf.d/default.conf
else
  proxy="location /API/ { proxy_pass http://${CONFIGURATORURL}/;}"
  sed -i -e 's@{proxyStting}@'"${proxy}"'@g' /etc/nginx/conf.d/default.conf
fi
