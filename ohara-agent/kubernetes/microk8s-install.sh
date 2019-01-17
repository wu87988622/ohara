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

yum install -y epel-release yum-plugin-copr sudo net-tools
yum copr -y enable ngompa/snapcore-el7
yum install -y snapd
ln -s /var/lib/snapd/snap /snap
systemctl start snapd
systemctl enable snapd
systemctl stop firewalld # In order to open the api server port, you can only open then 8080 port
setenforce 0 # SELinux must disable
snap install microk8s --classic
snap start microk8s
snap enable microk8s # for restart server to start microk8s service

# How to confirm microk8s service running?
# curl -X GET http://localhost:8080/api/v1/nodes
# You can watch the local node system information