# Ohara Manager

## How to install the MicroK8S for test?
* The MicroK8S must use root install

* Change to kubernetes folder from ohara-agent folder
```
# cd kubernetes
```

* Start install MicroK8S command
```
# bash micork8s-install.sh
```
please open the 8080 port for MicroK8S api server use

* Connect to MicroK8S api server
```
# curl -X GET http://localhost:8080/api/v1/nodes
```

如何安裝 distribute 模式的 kubernetes?

1. 安裝 kubernetes 的 master
    * 需要使用 root 的權限
    
    * 將資料夾切換到 ohara-agent/kubernetes/distribute 的路徑裡
    
    * 執行 bash k8s-master-install.sh ${Your host IP} 的指令來安裝 kubernetes 的 master, 參數需要指定本機的 IP
    
    * 安裝完成後到 /tmp/k8s-install-info.txt 的路徑查看 log 檔, 以及 token 和 hash 的資訊
    
2. 安裝 kubernetes 的 worker
    * 需要使用 root 的權限

    * 將資料夾切換到 ohara-agent/kubernetes/distribute 的路徑裡
    
    * 執行 bash k8s-worker-install.sh ${Your Master Host IP} ${Token} ${HASH_CODE} 的指令來安裝 kubernetes 的 worker, 參數主要是放 master 的 IP, TOKEN 和 HASH_CODE 可以在 master 的 /tmp/k8s-install-info.txt 檔案找到
    
3. 驗證安裝是否成功
    * 重新登入 master, 在 master 裡輸入 kubectl get nodes 指令, 查看 node 的狀態
    
    * 使用 curl -X GET http://${Your Master IP}:8080/api/v1/nodes 查看 node 的狀態
