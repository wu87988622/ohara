#!/bin/bash
if [ "$#" -ne 1 ]; then
    echo "Please assignment your host IP. ex: 10.0.0.101"
    exit 2
fi

SOURCE="${BASH_SOURCE[0]}"
BIN_DIR="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  BIN_DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
BIN_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

hostIP=$1

sudo bash $BIN_DIR/k8s-pre-install.sh
sudo yum install -y kubelet kubeadm kubectl
sudo systemctl enable kubelet
sudo systemctl start kubelet
sudo kubeadm init --apiserver-advertise-address=$hostIP --pod-network-cidr=10.244.0.0/16 > /tmp/k8s-install-info.txt # pod network is hard code
HOME_DIR=$HOME
sudo mkdir -p $HOME_DIR/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME_DIR/.kube/config
sudo chown $(id -u):$(id -g) $HOME_DIR/.kube/config
sudo kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
sudo systemctl restart kubelet
sudo kubectl proxy --accept-hosts=^*$ --address=$hostIP --port=8080 > /dev/null 2>&1 &
