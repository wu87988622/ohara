#!/bin/bash -eux
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


# Add vagrant user to sudoers.
echo "ohara        ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers

apt-get update
apt-get upgrade -y

timedatectl set-timezone Asia/Taipei

# Enable host-only adapter"
echo "" >> /etc/network/interfaces
echo "# host only interface
auto enp0s8
iface enp0s8 inet dhcp
" >> /etc/network/interfaces

apt-get install -y net-tools ifupdown network-manager
service network-manager start

# Init ohara account
su ohara
cd ~
sudo chmod u+x *.sh

echo "export OHARA_VER=$OHARA_VER" >> ~/.profile
echo "" >> ~/.profile
echo "./setup-ohara.sh" >> ~/.profile
