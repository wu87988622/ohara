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

.. _rest-stream:

StreamApp
=========

Ohara StreamApp is a unparalleled wrap of kafka streaming. It leverages
and enhances `Kafka Streams`_ to make
developer easily design and implement the streaming application. More
details of developing streaming application is in :ref:`custom stream guideline <streamapp>`.

Assume that you have completed a streaming application via ohara Java
APIs, and you have generated a jar including your streaming code. By
Ohara Restful APIs, you are enable to control, deploy, and monitor
your streaming application. As with cluster APIs, ohara leverages
docker container to host streaming application. Of course, you can
apply your favor container management tool including simple (based on ssh)
and k8s when you are starting ohara.

Before stating to use restful APIs, please ensure that all nodes have
downloaded the `StreamApp image`_.
The jar you uploaded to run streaming application will be included in
the image and then executes as a docker container. The `StreamApp image`_
is kept in each node so don’t worry about the network. We all hate
re-download everything when running services.

The following information of StreamApp are updated by ohara.

#. name (**string**) — custom name of this streamApp
#. group (**string**) — the value of group is always "default" (this logic apply to current version |version|)
#. imageName (**string**) — image name of this streamApp
#. instances (**int**) — numbers of streamApp container
#. nodeNames (**array(string)**) — node list of streamApp running container
#. deadNodes (**array(string)**) — dead node list of the exited containers from this cluster
#. jar (**object**) — uploaded jar key
#. from (**array(string)**) — topics of streamApp consume with
#. to (**array(string)**) — topics of streamApp produce to
#. state (**option(string)**) — only started/failed streamApp has state
#. jmxPort (**int**) — the expose jmx port
#. :ref:`metrics <connector-metrics>` (**object**) — the metrics from this streamApp.

   - meters (**array(object)**) — the metrics in meter type

     - meters[i].value (**double**) — the number stored in meter
     - meters[i].unit (**string**) — unit for value
     - meters[i].document (**string**) — document of this meter
     - meters[i].queryTime (**long**) — the time of query metrics from remote machine
     - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

#. exactlyOnce (**boolean**) — enable exactly once
#. error (**option(string)**) — the error message from a failed streamApp.
   If the streamApp is fine or un-started, you won’t get this field.
#. lastModified (**long**) — last modified this jar time


create properties of specific streamApp
---------------------------------------

Create the properties of a streamApp.

*POST /v0/stream*

Example Request
  #. name (**string**) — new streamApp name. This is the object unique name.

     - The acceptable char is [0-9a-z]
     - The maximum length is 20 chars

  #. imageName (**option(string)**) — image name of streamApp used to ;
     default is official streamapp image of current version
  #. jar (**object**) — the used jar object

     - jar.group (**string**) — the group name of this jar
     - jar.name (**string**) — the name without extension of this jar

  #. from (**option(array(string))**) — new source topics ; default is empty
  #. to (**option(array(string))**) — new target topics ; default is empty
  #. jmxPort (**option(int)**) — expose port for jmx ; default is random port
  #. instances (**option(int)**) — number of running streamApp ; default is 1
  #. nodeNames (**option(array(string))**) — node name list of streamApp used to ; default is empty

  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "oharastream/streamapp:$|version|",
       "jar": {
         "group": "wk01",
         "name": "stream-app"
       },
       "from": [
         "topic1"
       ],
       "to": [
         "topic2"
       ],
       "jmxPort": 5678,
       "instances": 3,
       "nodeNames": []
     }

Example Response
  #. name (**string**) — custom name of this streamApp
  #. group (**string**) — the value of group is always "default" (this logic apply to current version |version|)
  #. imageName (**string**) — image name of this streamApp
  #. instances ( **int**) — numbers of streamApp container
  #. nodeNames (**array(string)**) — node list of streamApp running
     container
  #. deadNodes (**array(string)**) — dead node list of the exited
     containers from this cluster
  #. jar (**object**) — uploaded jar key
  #. from (**array(string)**) — topics of streamApp consume with
  #. to (**array(string)**) — topics of streamApp produce to
  #. state (**option(string)**) — only started/failed streamApp has state
  #. jmxPort (**int**) — the expose jmx port
  #. :ref:`metrics <connector-metrics>` (**object**) — the metrics from this streamApp.

     - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of query metrics from remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

  #. exactlyOnce (**boolean**) — enable exactly once
  #. error (**option(string)**) — the error message from a failed
     streamApp. If the streamApp is fine or un-started, you won’t get
     this field.
  #. lastModified (**long**) — last modified this jar time
  #. exactlyOnce (**boolean**) — enable exactly once
  #. error (**option(string)**) — the error message from a failed
     streamApp. If the streamApp is fine or un-started, you won’t get
     this field.
  #. lastModified (**long**) — last modified this jar time
  #. tags (**object**) — user defined data

  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "oharastream/streamapp:$|version|",
       "instances": 3,
       "nodeNames": [],
       "deadNodes": [],
       "jar": {
         "name": "stream-app",
         "group": "wk01"
       },
       "from": [
         "topic1"
       ],
       "to": [
         "topic2"
       ],
       "jmxPort": 5678,
       "exactlyOnce": "false",
       "metrics": [],
       "lastModified": 1542102595892
     }

  .. note::
     The streamApp, which is just created, does not have any metrics.


.. _rest-stream-get-information:

get information from a specific streamApp cluster
-------------------------------------------------

*GET /v0/stream/${name}*

Example Response
  #. name (**string**) — custom name of this streamApp
  #. group (**string**) — the value of group is always "default"
     (this logic apply to current version |version|)
  #. imageName (**string**) — image name of this streamApp
  #. instances ( **int**) — numbers of streamApp container
  #. nodeNames (**array(string)**) — node list of streamApp running
     container
  #. deadNodes (**array(string)**) — dead node list of the exited
     containers from this cluster
  #. jar (**object**) — uploaded jar key
  #. from (**array(string)**) — topics of streamApp consume with
  #. to (**array(string)**) — topics of streamApp produce to
  #. state (**option(string)**) — only started/failed streamApp has state
  #. jmxPort (**int**) — the expose jmx port
  #. :ref:`metrics <connector-metrics>` (**object**) — the metrics from this streamApp.

     - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of record generated in remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

  #. exactlyOnce (**boolean**) — enable exactly once
  #. error (**option(string)**) — the error message from a failed
     streamApp. If the streamApp is fine or un-started, you won’t get
     this field.
  #. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "oharastream/streamapp:$|version|",
       "instances": 3,
       "nodeNames": [],
       "deadNodes": [],
       "jar": {
         "name": "stream-app",
         "group": "wk01"
       },
       "from": [
         "topic1"
       ],
       "to": [
         "topic2"
       ],
       "jmxPort": 5678,
       "exactlyOnce": "false",
       "metrics": [],
       "lastModified": 1542102595892
     }

list information of streamApp cluster
-------------------------------------

*GET /v0/stream*

Example Response
  #. name (**string**) — custom name of this streamApp
  #. group (**string**) — the value of group is always "default"
     (this logic apply to current version |version|)
  #. imageName (**string**) — image name of this streamApp
  #. instances ( **int**) — numbers of streamApp container
  #. nodeNames (**array(string)**) — node list of streamApp running
     container
  #. deadNodes (**array(string)**) — dead node list of the exited
     containers from this cluster
  #. jar (**object**) — uploaded jar key
  #. from (**array(string)**) — topics of streamApp consume with
  #. to (**array(string)**) — topics of streamApp produce to
  #. state (**option(string)**) — only started/failed streamApp has state
  #. jmxPort (**int**) — the expose jmx port
  #. :ref:`metrics <connector-metrics>` (**object**) — the metrics from this streamApp.

     - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of record generated in remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

  #. exactlyOnce (**boolean**) — enable exactly once
  #. error (**option(string)**) — the error message from a failed
     streamApp. If the streamApp is fine or un-started, you won’t get
     this field.
  #. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     [
       {
         "name": "myapp",
         "group": "default",
         "imageName": "oharastream/streamapp:$|version|",
         "instances": 3,
         "nodeNames": [],
         "deadNodes": [],
         "jar": {
           "name": "stream-app",
           "group": "wk01"
         },
         "from": [
           "topic1"
         ],
         "to": [
           "topic2"
         ],
         "jmxPort": 5678,
         "exactlyOnce": "false",
         "metrics": [],
         "lastModified": 1542102595892
       }
     ]

update properties of specific streamApp
---------------------------------------

Update the properties of a non-started streamApp.

*PUT /v0/stream/${name}*

Example Request
  #. imageName (**option(string)**) — new streamApp image name
  #. from (**option(array(string))**) — new source topics
  #. to (**option(array(string))**) — new target topics
  #. jar (**option(object)**) — new uploaded jar key
  #. jmxPort (**option(int)**) — new jmx port
  #. instances (**option(int)**) — new number of running streamApp
  #. nodeNames (**option(array(string))**) — new node name list of
     streamApp used to (this field has higher priority than instances)

  .. code-block:: json

     {
       "imageName": "myimage",
       "from": [
         "newTopic1"
       ],
       "to": [
         "newTopic2"
       ],
       "jar": {
         "group": "newGroup",
         "name": "newJar"
       },
       "jmxPort": 8888,
       "instances": 3,
       "nodeNames": ["node1", "node2"]
     }

Example Response
  #. name (**string**) — custom name of this streamApp
  #. group (**string**) — the value of group is always "default" (this logic apply to current version |version|)
  #. imageName (**string**) — image name of this streamApp
  #. instances ( **int**) — numbers of streamApp container
  #. nodeNames (**array(string)**) — node list of streamApp running
     container
  #. deadNodes (**array(string)**) — dead node list of the exited
     containers from this cluster
  #. jar (**object**) — uploaded jar key
  #. from (**array(string)**) — topics of streamApp consume with
  #. to (**array(string)**) — topics of streamApp produce to
  #. state (**option(string)**) — only started/failed streamApp has state
  #. jmxPort (**int**) — the expose jmx port
  #. :ref:`metrics <connector-metrics>` (**object**) — the metrics from this streamApp.

     - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of query metrics from remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine
  #. exactlyOnce (**boolean**) — enable exactly once
  #. error (**option(string)**) — the error message from a failed
     streamApp. If the streamApp is fine or un-started, you won’t get
     this field.
  #. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "myimage",
       "instances": 2,
       "nodeNames": ["node1", "node2"],
       "deadNodes": [],
       "jar": {
         "name": "stream-app",
         "group": "wk01"
       },
       "from": [
         "newTopic1"
       ],
       "to": [
         "newTopic2"
       ],
       "jmxPort": 8888,
       "exactlyOnce": "false",
       "metrics": [],
       "lastModified": 1542102595892
     }


delete properties of specific streamApp
---------------------------------------

Delete the properties of a non-started streamApp. This api only remove
the streamApp component which is stored in pipeline.

*DELETE /v0/stream/${name}*

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent properties, and the response is 204
     NoContent.


start a StreamApp
-----------------

*PUT /v0/stream/${name}/start*

Example Response
  ::

    202 Accepted

  .. note::
     You should use :ref:`get streamapp <rest-stream-get-information>` to fetch up-to-date status

  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "oharastream/streamapp:$|version|",
       "instances": 1,
       "nodeNames": ["node1"],
       "deadNodes": [],
       "jar": {
         "name": "streamapp",
         "group": "wk01"
       },
       "from": [
         "topicA"
       ],
       "to": [
         "topicB"
       ],
       "state": "RUNNING",
       "jmxPort": 5678,
       "exactlyOnce": "false",
       "metrics": [],
       "lastModified": 1542102595892
     }

.. _rest-stop-streamapp:

stop a StreamApp
----------------

This action will graceful stop and remove all docker containers belong
to this streamApp. Note: successful stop streamApp will have no status.

*PUT /v0/stream/${name}/stop*

Example Response
  ::

    202 Accepted

  .. note::

     You should use :ref:`get streamapp <rest-stream-get-information>` to fetch up-to-date status


  .. code-block:: json

     {
       "name": "myapp",
       "group": "default",
       "imageName": "oharastream/streamapp:$|version|",
       "instances": 1,
       "nodeNames": ["node1"],
       "deadNodes": [],
       "jar": {
         "name": "streamapp",
         "group": "wk01"
       },
       "from": [
         "topicA"
       ],
       "to": [
         "topicB"
       ],
       "jmxPort": 5678,
       "exactlyOnce": "false",
       "metrics": [],
       "lastModified": 1542102595892
     }

get topology tree graph from specific streamApp
-----------------------------------------------

[TODO] This is not implemented yet !

*GET /v0/stream/view/${name}*

Example Response
  #. jarInfo (**string**) — the upload jar information
  #. name (**string**) — the streamApp name
  #. poneglyph (**object**) — the streamApp topology tree graph

      - steles (**array(object)**) — the topology collection

         - steles[i].kind (**string**) — this component kind (SOURCE,
           PROCESSOR, or SINK)
         - steles[i].key (**string**) — this component kind with order
         - steles[i].name (**string**) — depend on kind, the name is

            - SOURCE — source topic name
            - PROCESSOR — the function name
            - SINK — target topic name

         - steles[i].from (**string**) — the prior component key (could be
           empty if this is the first component)
         - steles[i].to (**string**) — the posterior component key (could be
           empty if this is the final component)

  .. code-block:: json

     {
       "jarInfo": {
         "name": "stream-app",
         "group": "wk01",
         "size": 1234,
         "lastModified": 1542102595892
       },
       "name": "my-app",
       "poneglyph": {
         "steles": [
           {
             "kind": "SOURCE",
             "key" : "SOURCE-0",
             "name": "stream-in",
             "from": "",
             "to": "PROCESSOR-1"
           },
           {
             "kind": "PROCESSOR",
             "key" : "PROCESSOR-1",
             "name": "filter",
             "from": "SOURCE-0",
             "to": "PROCESSOR-2"
           },
           {
             "kind": "PROCESSOR",
             "key" : "PROCESSOR-2",
             "name": "mapvalues",
             "from": "PROCESSOR-1",
             "to": "SINK-3"
           },
           {
             "kind": "SINK",
             "key" : "SINK-3",
             "name": "stream-out",
             "from": "PROCESSOR-2",
             "to": ""
           }
         ]
       }
     }

.. _Kafka Streams: kafka streams <https://kafka.apache.org/documentation/streams
.. _StreamApp image: https://cloud.docker.com/u/oharastream/repository/docker/oharastream/streamapp