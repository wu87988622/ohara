#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo 'Your input format error. Ex: bash k8s-worker-install.sh 10.0.0.101 ${TOKEN} ${HASH_CODE}'
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

bash $BIN_DIR/k8s-pre-install.sh
masterHostIP=$1
token=$2
hashCode=$3

sudo yum install -y kubelet kubeadm kubectl
sudo systemctl enable kubelet
sudo systemctl start kubelet
sudo kubeadm join $masterHostIP:6443 --token $token --discovery-token-ca-cert-hash $hashCode