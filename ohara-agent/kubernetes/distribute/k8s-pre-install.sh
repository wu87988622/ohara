#!/bin/bash
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