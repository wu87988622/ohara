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

FROM nginx

ARG NGINXPATH=/usr/share/nginx
COPY swagger/dist $NGINXPATH/html
COPY swagger/yamls $NGINXPATH/html/yamls
COPY swagger/generate_json.sh $NGINXPATH/html
COPY swagger/nginx.conf /etc/nginx/default.conf
COPY swagger/set_proxy.sh /etc/nginx/

RUN bash /usr/share/nginx/html/generate_json.sh

CMD ["bash","-c","/etc/nginx/set_proxy.sh && nginx -g \"daemon off;\""]