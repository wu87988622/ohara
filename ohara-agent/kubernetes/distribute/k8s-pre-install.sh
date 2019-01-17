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

yum install -y https://yum.dockerproject.org/repo/main/centos/7/Packages/docker-engine-17.05.0.ce-1.el7.centos.x86_64.rpm
systemctl enable docker
systemctl start docker
systemctl disable firewalld
systemctl stop firewalld # You can only open the 6443 and 8080 port
sed -i s/^SELINUX=.*$/SELINUX=disabled/ /etc/selinux/config
setenforce 0 # SELinux must disable
sed -i '/ swap / s/^/#/' /etc/fstab
swapoff -a
echo '1' > /proc/sys/net/bridge/bridge-nf-call-iptables
echo -e "[kubernetes]\nname=Kubernetes\nbaseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64\nenabled=1\ngpgcheck=1\nrepo_gpgcheck=1\ngpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg\n       https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg\n">/etc/yum.repos.d/kubernetes.repo