#!/bin/bash
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