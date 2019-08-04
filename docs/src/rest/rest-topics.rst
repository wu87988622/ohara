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

.. _rest-topics:

Topic
=====

Ohara topic is based on kafka topic. It means the creation of topic on
ohara will invoke a creation of kafka also. Also, the delete to ohara
topic also invoke a delete request to kafka. The common properties in
topic are shown below.

#. group (**string**) — topic group.
#. name (**string**) — topic name
#. brokerClusterName (**option(string)**) — the broker cluster hosting
   this topic (**If you don’t specify the broker cluster in request,
   Ohara will try to find a broker cluster for you. And it works only if
   there is only a broker cluster exists in Ohara**)
#. numberOfReplications (**option(int)**) — the number of replications
   for this topic (**it is illegal to input the number of replications
   which is larger than the number of broker nodes**)
#. numberOfPartitions (**option(int)**)— the number of partitions for
   this topic
#. configs (**option(object)**) — the custom configs used to create topic
#. state (**option(string)**) — state of a running topic. nothing if the topic is not running.
#. tags (**option(object)**) — the extra description to this
   object

.. note::
   The name must be unique in a broker cluster.

The following information are tagged by ohara.

#. group (**string**) — the group value is always "default" (the default
   value will be changed to be equal to brokerClusterName as the group
   of topic is “broker cluster”)
#. lastModified (**long**) — the last time to update this ftp
   information


store a topic properties
------------------------

*POST /v0/topics*

#. group (**string**) — topic group. Default group is "default".
#. name (**string**) — topic name
#. brokerClusterName (**option(string)**) — the broker cluster hosting
   this topic (**If you don’t specify the broker cluster in request,
   ohara will try to find a broker cluster for you. And it works only if
   there is only a broker cluster exists in ohara**)
#. numberOfReplications (**option(int)**) — the number of replications
   for this topic (**it is illegal to input the number of replications
   which is larger than the number of broker nodes**)
#. numberOfPartitions (**option(int)**)— the number of partitions for
   this topic
#. configs (**option(object)**) — the custom configs used to create topic
#. state (**option(string)**) — state of a running topic. nothing if the topic is not running.
#. tags (**option(object)**) — the extra description to this
   object

.. note::
  #. the name you pass to ohara is used to build topic on kafka, and it is restricted by Kafka ([a-zA-Z0-9\._\-])
  #. the ignored fields will be auto-completed by Ohara Configurator. Also, you could update/replace it by UPDATE request later.
  #. this API does NOT create a topic on broker cluster. Instead, you should sent START request to run a topic on broker cluster actually

Example Request
  .. code-block:: json

     {
       "name": "topic0",
       "numberOfReplications": 1,
       "numberOfPartitions": 1
     }


Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "topic0",
       "brokerClusterName": "preCreatedBkCluster",
       "lastModified": 1553498552595,
       "numberOfReplications": 1,
       "numberOfPartitions": 1,
       "metrics": {
         "meters": []
       },
       "configs": {},
       "tags": {}
     }

  .. note::
     The topic, which is just created, does not have any metrics.


update a topic properties
-------------------------

*PUT /v0/topics/${name}?group=${group}*

1. numberOfPartitions (**int**) — the number of partitions for this
   topic (**it is illegal to decrease the number**)
2. tags (**array(string)**) — the extra description to this object

Both number of replications and configs are unmodifiable. An error response is produced if the update request tries to
update them.

Example Request
  .. code-block:: json

     {
       "numberOfPartitions": 3
     }


Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "topic0",
       "brokerClusterName": "preCreatedBkCluster",
       "lastModified": 1553498552595,
       "numberOfReplications": 1,
       "numberOfPartitions": 3,
       "metrics": {
        "meters": []
       },
       "configs": {},
       "tags": {}
     }


list all topics properties
--------------------------

*GET /v0/topics*

Example Response
  .. code-block:: json

     [
       {
         "group": "default",
         "name": "topic0",
         "brokerClusterName": "preCreatedBkCluster",
         "lastModified": 1553498552595,
         "numberOfReplications": 1,
         "numberOfPartitions": 1,
         "metrics": {
          "meters": []
         },
         "configs": {},
         "tags": {}
       },
       {
         "group": "default",
         "name": "wk00",
         "brokerClusterName": "preCreatedBkCluster",
         "lastModified": 1553498375573,
         "numberOfReplications": 1,
         "numberOfPartitions": 1,
         "metrics": {
          "meters": []
         },
         "configs": {},
         "tags": {}
       }
     ]


delete a topic properties
-------------------------

*DELETE /v0/topics/${name}?group=${group}*

Example Response

  ::

     204 NoContent

  .. note::
    It is ok to delete an nonexistent topic, and the response is 204 NoContent.


get a topic properties
----------------------

*GET /v0/topics/${name}*

Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "topic0",
       "brokerClusterName": "preCreatedBkCluster",
       "lastModified": 1553498552595,
       "numberOfReplications": 1,
       "numberOfPartitions": 1,
       "metrics": {
        "meters": []
       },
       "configs": {},
       "tags": {}
     }


start a topic on remote broker cluster
--------------------------------------

*PUT /v0/topics/${name}/start*


Example Response
  ::

     202 Accepted


stop a topic from remote broker cluster
---------------------------------------

*PUT /v0/topics/${name}/stop*

.. note::
  the topic will lose all data after stopping.

Example Response
  ::

     202 Accepted


