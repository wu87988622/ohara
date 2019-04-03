# Kubernetes install guide

## How to install distribute mode for Kubernetes?
### 1.Install Kubernetes master
  * Switch to root user
```
$ su root
```

  * Change directory to ```--> kubernetes/distribute```
```
# cd $OHARA_HOME/kubernetes/distribute
```

  * Run ```bash k8s-master-install.sh ${Your_K8S_Master_Host_IP}``` to install Kubernetes master
```
# bash k8s-master-install.sh ${Your_K8S_Master_Host_IP}
```

  * Token and hash will be used in worker installation later on
```
# cat /tmp/k8s-install-info.txt
```

The token and hash should look like the following:
```
# kubeadm join 10.100.0.178:6443 --token 14aoza.xpgpa26br32sxwl8 \
    --discovery-token-ca-cert-hash sha256:f5614e6b6376f7559910e66bc014df63398feb7411fe6d0e7057531d7143d47b
```
**Token:** 14aoza.xpgpa26br32sxwl8

**Hash:** sha256:f5614e6b6376f7559910e66bc014df63398feb7411fe6d0e7057531d7143d47b
  
### 2.Install Kubernetes worker
  * Switch to root
```
$ su root
```
  * Change directory to ```--> kubernetes/distribute```
```
# cd $OHARA_HOME/kubernetes/distribute 
```

  * Run ```bash k8s-worker-install.sh ${Your_K8S_Master_Host_IP} ${TOKEN} ${HASH_CODE}``` command in your terminal. (TOKEN and HASH_CODE can be found in the /tmp/k8s-install-info.txt file of Kubernetes master, the one we mention in the previous steps)
    
    Below is example command:
```
# bash k8s-worker-install.sh 10.100.0.178 14aoza.xpgpa26br32sxwl8 sha256:f5614e6b6376f7559910e66bc014df63398feb7411fe6d0e7057531d7143d47b
```  

**3.Ensure the K8S API server is running properly**
  * Log into Kubernetes master and use the following command to see if these Kubernetes nodes are running properly

```
# kubectl get nodes
```
  * You can check Kubernetes node status like the following:
```
# curl -X GET http://${Your_K8S_Master_Host_IP}:8080/api/v1/nodes
```
## How to use Kubernetes in OharaStream?
  * You must create the service to Kubernetes for DNS use in kubernetes master host, Below is the command:
```
cd $OHARA_HOME/kubernetes
kubectl create -f dns-service.yaml
```
  * Below is an example command:
```
# docker run --rm \
           -p 5000:5000 \
           --add-host ${K8S_WORKER01_HOSTNAME}:${K8S_WORKER01_IP} \
           --add-host ${K8S_WORKER02_HOSTNAME}:${K8S_WORKER02_IP} \
           oharastream/configurator:0.4-SNAPSHOT \
           --port 5000 \
           --hostname ${Start Configurator Host Name} \
           --k8s http://${Your_K8S_Master_Host_IP}:8080/api/v1
```
*--add-host: Add all k8s worker hostname and ip information to configurator container /etc/hosts file

*--k8s: Assignment your K8S API server HTTP URL

  * Use Ohara configurator to create zookeeper and broker in Kubernetes pod for the test:
```
# Add Ohara Node example
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{"name": "${K8S_WORKER01_HOSTNAME}", \ 
          "port": 22, \
          "user": "${USERNAME}", \ 
          "password": "${PASSWORD}"}' \ 
     http://${CONFIGURATOR_HOST_IP}:5000/v0/nodes

curl -H "Content-Type: application/json" \ 
     -X POST \
     -d '{"name": "${K8S_WORKER02_HOSTNAME}", \ 
          "port": 22, \
          "user": "${USERNAME}", \
          "password": "${PASSWORD}"}' \
     http://${CONFIGURATOR_HOST_IP}:5000/v0/nodes

# You must pre pull docker image in the ${K8S_WORKER01_HOSTNAME} and ${K8S_WORKER02_HOSTNAME} host, Below is command:
docker pull oharastream/zookeeper:0.4-SNAPSHOT
docker pull oharastream/broker:0.4-SNAPSHOT

# Create Zookeeper service example
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{"name": "zk", \
          "clientPort": 2181, \
          "imageName": "oharastream/zookeeper:0.4-SNAPSHOT", \
          "peerPort": 2000, \
          "electionPort": 2001, \
          "nodeNames": ["${K8S_WORKER01_HOSTNAME}"]}' \
     http://${CONFIGURATOR_HOST_IP}:5000/v0/zookeepers

# Create Broker service example
curl -H "Content-Type: application/json" \
     -X POST \
     -d '{"name": "bk", \
          "clientPort": 9092, \
          "zookeeperClusterName": "zk", \
          "nodeNames": ["${K8S_WORKER02_HOSTNAME}"]}' \
     http://${CONFIGURATOR_HOST_IP}:5000/v0/brokers
```
  * You can use the kubectl command to confirm zookeeper and broker pod status:
```
# kubectl get pods
```