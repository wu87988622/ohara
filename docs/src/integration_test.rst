..
.. Copyright 2019 is-land
..
.. Licensed under the Apache License, Version 2.0 (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
..     http://www.apache.org/licenses/LICENSE-2.0
..
.. Unless required by applicable law or agreed to in writing, software
.. distributed under the License is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
..

Integration Test
================

**How to deploy ohara integration test to QA environment?**

**Node 裡需要安裝以下的工具:**

-  安裝 JDK 1.8, 需要設定以下的 link

::

   $ sudo yum install -y java-1.8.0-openjdk-devel

-  在 CentOS 的 Node 需要安裝 jq

::

   $ sudo yum install -y epel-release
   $ sudo yum install -y jq

-  ssh server

::

   $ sudo yum install -y openssh-server

-  安裝 Docker

Please follow `docker official tutorial`_

-  防火牆設定允許 docker container 的 port, 並且重新 reload 防火牆的服務

::

   # sudo firewall-cmd --permanent --zone=trusted --add-interface={docker network}
   # sudo firewall-cmd --reload

**Jenkins 需要做以下的設定**

1.確認 jenkins 是否加入了登入的帳號密碼設定

::

   Credentials -> global -> Add Credentials -> 輸入 Username, Password, Description. ID 不用輸入 -> OK

2.把 Node 加入到 Jenkins 裡

::

   管理 Jenkins -> 管理節點 -> 新增節點 -> 輸入節點名稱的 hostname -> 選複製既有節點
   -> 複製來源選一台現有的 slave 來輸入, 例如：ohara-it01 -> OK

3.把 Node 加入到 ssh remote hosts

::

   管理 Jenkins -> 設定系統 -> SSH remote hosts -> 往下拉會看到新增按鈕 -> 之後輸入 Hostname, port, 選 Credentials -> 新增

4.設定 PreTest 和 PreCommit

::

       (1) 修改標籤表示式, 避免 IT 的 node 跑到 UT 上面

       (2) 增加 node 在 shell script 裡
           NODE01_HOSTNAME="ohara-it02"
           NODE01_IP=$(getent hosts $NODE01_HOSTNAME | cut -d" " -f 1)
           NODE01_INFO="$NODE_USER_NAME:$NODE_PASSWORD@$NODE01_HOSTNAME:22"

           EXTRA_PROPERTIES="\
           -Pohara.it.docker=$NODE00_INFO,$NODE01_INFO \

       (3) 建立 Execute shell script on remote host using ssh, 用來在 IT 的 Node 拉 docker image
           新增建置步驟 -> Execute shell script on remote host using ssh -> 輸入拉 docker image 的 command

.. _`docker official tutorial`: https://docs.docker.com/install/linux/docker-ce/centos/