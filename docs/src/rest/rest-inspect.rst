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


Inspect
=======

Inspect APIs is a powerful tool that it enable you to "see" what in the target via Configurator. For example, you can
get the definitions from specific image, or you can see what connectors or stream app in the specific file.

get Ohara Configurator info
---------------------------

*GET /v0/inspect/configurator*

the format of response of Ohara Configurator is shown below.

#. versionInfo (**object**) — version details of Ohara Configurator

   - branch (**string**) — the branch name of Ohara Configurator
   - version (**string**) — the release version of Ohara Configurator
   - revision (**string**) — commit hash of Ohara Configurator. You can trace the hash code via `Github <https://github.com/oharastream/ohara/commits/master>`__
   - user (**string**) — the release manager of Ohara Configurator.
   - date (**string**) — the date of releasing Ohara Configurator.

#. mode (**string**) — the mode of this configurator. There are three modes now:

   - K8S: k8s mode is for the production.
   - SSH: ssh is useful to simple env.
   - FAKE: fake mode is used to test APIs.

Example Response
  .. code-block:: json

    {
      "versionInfo": {
        "branch": "$|branch|",
        "version": "$|version|",
        "user": "chia",
        "revision": "b86742ca03a0ca02cc3578f8686e38e5cf2fb461",
        "date": "2019-05-13 09:59:38"
      },
      "mode": "FAKE"
    }

get zookeeper/broker/worker/stream info
---------------------------------------

*GET /v0/inspect/$service*

This API used to fetch the definitions for specific cluster service. The following fields are returned.

#. imageName (**string**) — the image name of service
#. settingDefinitions (**array(object)**) — the available settings for this service (see :ref:`setting <setting-definition>`)

the available variables for $service are shown below.

#. zookeeper
#. broker
#. worker
#. stream

Example Response
  .. code-block:: json

    {
      "imageName": "oharastream/zookeeper:$|version|",
      "settingDefinitions": [
        {
          "reference": "NONE",
          "displayName": "group",
          "internal": false,
          "documentation": "group of this worker cluster",
          "valueType": "STRING",
          "tableKeys": [],
          "orderInGroup": 1,
          "key": "group",
          "required": false,
          "defaultValue": "default",
          "group": "core",
          "editable": true
        }
      ]
    }

get running zookeeper/broker/worker/stream info
-----------------------------------------------

*GET /v0/inspect/$service/name=$name?group=$group*

This API used to fetch the definitions for specific cluster service and the definitions of available classes in the service.
The following fields are returned.

#. imageName (**string**) — the image name of service
#. settingDefinitions (**array(object)**) — the available settings for this service (see :ref:`setting <setting-definition>`)
#. classInfos (**array(object)**) — the information available classes in this service

  - classInfos[i].className — the name of this class
  - classInfos[i].classType — the type of this class. for example, topic, source connector, sink connector or stream app
  - classInfos[i].settingDefinitions — the definitions of this class

the available variables for $service are shown below.

#. zookeeper
#. broker
#. worker
#. stream

Example Response
  .. code-block:: json

    {
      "imageName": "oharastream/streamapp:$|version|",
      "settingDefinitions": [
        {
          "reference": "NONE",
          "displayName": "group",
          "internal": false,
          "documentation": "group of this worker cluster",
          "valueType": "STRING",
          "tableKeys": [],
          "orderInGroup": 1,
          "key": "group",
          "required": false,
          "defaultValue": "default",
          "group": "core",
          "editable": true
        }
      ],
      "classInfos": [

      ]
    }

Query Database
--------------

*POST /v0/inspect/rdb*

This API returns the table details of a relational database. This API
invokes a running connector on worker cluster to fetch database
information and return to Ohara Configurator. You should deploy suitable
jdbc driver on worker cluster before using this API. Otherwise, you will
get a exception returned by Ohara Configurator. The query consists of
following fields.

#. url (**string**) — jdbc url
#. user (**string**) — user who can access target database
#. password (**string**) — password which can access target database
#. workerClusterKey (**Object**) — target worker cluster.

   - workerClusterKey.group (**option(string)**) — the group of cluster
   - workerClusterKey.name (**string**) — the name of cluster

  .. note::
    the following forms are legal as well. 1) {"name": "n"} and 2) "n". Both forms are converted to
    {"group": "default", "name": "n"}

#. catalogPattern (**option(string)**) — filter returned tables according to catalog
#. schemaPattern (**option(string)**) — filter returned tables according to schema
#. tableName (**option(string)**) — filter returned tables according to name

Example Request
  .. code-block:: json

     {
       "url": "jdbc:sqlserver://",
       "user": "abc",
       "password": "abc",
       "workerClusterKey": {
         "group": "default",
         "name": "wk00"
       }
     }

Example Response
  #. name (**string**) — database name
  #. tables (**array(object)**)

     - tables[i].catalogPattern (**option(object)**) — table’s catalog pattern
     - tables[i].schemaPattern (**option(object)**) — table’s schema pattern
     - tables[i].name (**option(object)**) — table’s name
     - tables[i].columns (**array(object)**) — table’s columns

       - tables[i].columns[j].name (**string**) — column’s columns
       - tables[i].columns[j].dataType (**string**) — column’s data type
       - tables[i].columns[j].pk (**boolean**) — true if this column is pk. otherwise false

  .. code-block:: json

     {
       "name": "sqlserver",
       "tables": [
         {
           "name": "t0",
           "columns": [
             {
               "name": "c0",
               "dataType": "integer",
               "pk": true
             }
           ]
         }
       ]
     }


Query Topic
--------------

*POST /v0/inspect/topic/$name?group=$group&timeout=$timeout&$limit=$limit*

Fetch the latest data from a topic. the query arguments are shown below.

#. timeout (**long**) — break the fetch if this timeout is reached
#. limit (**long**) — the number of messages in topic

the response includes following items.

#. messages (**Array(Object)**) — messages

  - messages[i].partition (**int**) — the index of partition
  - messages[i].offset (**Long**) — the offset of this message
  - messages[i].sourceClass (**Option(String)**) — class name of the component which generate this data
  - messages[i].sourceKey (**Option(Object)**) — object key of the component which generate this data
  - messages[i].value (**Option(Object)**) — the value of this message
  - messages[i].error (**Option(String)**) — error message happen in failing to parse value

Example Response

.. code-block:: json

  {
    "messages": [
      {
        "partition": 1,
        "offset": 12,
        "sourceClass": "com.abc.SourceTask",
        "sourceKey": {
          "group": "g",
          "name": "n"
        },
        "value": {
          "a": "b",
          "b": "c"
        }
      },
      {
        "partition": 1,
        "offset": 13,
        "error": "unknown message"
      }
    ]
  }

Query File
-----------

#. name (**string**) — the file name without extension
#. group (**string**) — the group name (we use this field to separate different workspaces)
#. size (**long**) — file size
#. classInfos (**array(object)**) — the information of available classes in this file

  - classInfos[i].className — the name of this class
  - classInfos[i].classType — the type of this class. for example, topic, source connector, sink connector or stream app
  - classInfos[i].settingDefinitions — the definitions of this class

#. lastModified (**long**) — the time of uploading this file

*POST /v0/inspect/files*

Example Request
  .. code-block:: text

     Content-Type: multipart/form-data
     file="aa.jar"
     group="wk01"


Example Response

  .. code-block:: json

    {
      "name": "aa.jar",
      "group": "wk01",
      "size": 1779,
      "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
      "classInfos": [
        {
          "classType": "connector",
          "className": "a.b.c.Source",
          "settingDefinitions": []
        },
        {
          "classType": "streamApp",
          "className": "a.b.c.bbb",
          "settingDefinitions": []
        }
      ],
      "lastModified": 1561012496975
    }
