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


Container
=========

Each processes managed by Ohara is based on docker container. In most
cases, user don’t need to know the details of containers since the
management of containers is on Ohara’s shoulder. However, Ohara
understand that we all have curious brain so Ohara supports to display
the container’s details of a running cluster. Noted that the context may
be changed between different release of Ohara. And the distinct
implementations of container manager possibly provide different context
of containers.


retrieve the container details of a running cluster
---------------------------------------------------

*GET /v0/containers/$clusterName*

Example Response
  The **cluster name** may be mapped to different services (of course, it
  would be better to avoid using same name on different services), hence,
  the returned JSON is in array type. The details of elements are shown
  below.

  #. clusterName (**string**) — cluster name
  #. clusterType (**string**) — cluster type
  #. containers (**array(object)**) — the container in this cluster

     - environments (**object**) — the environment variables of container
     - name (**string**) — the name of container
     - hostname (**string**) — hostname of container
     - size (**string**) — the disk size used by this container
     - state (**option(string)**) — the state of container
     - portMappings (**array(object)**) — the exported ports of this container

       - portMappings[i].hostIp (**string**) — the network interface of container host
       - portMappings[i].portPairs (**object**) — the container port and host port

         - portMappings[i].portPairs[j].hostPort (**int**) — host port
         - portMappings[i].portPairs[j].containerPort (**int**) — container port

     - nodeName (**string**) — the node which host this container
     - imageName (**string**) — the image used to create this container
     - id (**string**) — container id
     - created (**string**) — create time

  .. code-block:: json

     [
       {
         "clusterName": "zk00",
         "clusterType": "zookeeper",
         "containers": [
           {
             "environments": {
               "PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/home/zookeeper/default/bin",
               "ZK_ID": "0",
               "ZK_ELECTION_PORT": "3888",
               "JAVA_HOME": "/usr/lib/jvm/jre",
               "ZK_CLIENT_PORT": "2181",
               "ZK_SERVERS": "node00",
               "ZK_PEER_PORT": "2888",
               "ZOOKEEPER_HOME": "/home/zookeeper/default"
             },
             "name": "occl-zk00-zk-2aa11cc",
             "hostname": "node00",
             "size": "32.9kB (virtual 595MB)",
             "state": "RUNNING",
             "portMappings": [
               {
                 "hostIp": "0.0.0.0",
                 "portPairs": [
                   {
                     "hostPort": 2181,
                     "containerPort": 2181
                   },
                   {
                     "hostPort": 2888,
                     "containerPort": 2888
                   },
                   {
                     "hostPort": 3888,
                     "containerPort": 3888
                   }
                 ]
               }
             ],
             "nodeName": "node00",
             "imageName": "oharastream/zookeeper:$|version|",
             "id": "22169c48646c",
             "kind": "SSH",
             "created": "2019-04-12 03:30:56 -0400 EDT"
           }
         ]
       }
     ]

