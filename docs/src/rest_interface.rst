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

.. _rest:

Ohara REST Interface
====================

ohara provides a bunch of REST APIs of managing data, applications and
cluster for ohara users. Both request and response must have
application/json content type, hence you should set content type to
application/json in your request.

::

   Content-Type: application/json

and add content type of the response via the HTTP Accept header:

::

   Accept: application/json


Statuses & Errors
-----------------

ohara leverages akka-http to support standards-compliant HTTP statuses.
your clients should check the HTTP status before parsing response
entities. The error message in response body are format to json content.

.. code-block:: json

   {
     "code": "java.lang.IllegalArgumentException",
     "message": "Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd",
     "stack": "java.lang.IllegalArgumentException: Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd at"
   }

1. code (**string**) — the type of error. It is normally a type of java
   exception
2. message (**string**) — a brief description of error
3. stack (**string**) — error stack captured by server

Manage clusters
---------------

You are tired to host a bunch of clusters when you just want to build a
pure streaming application. So do we! Ohara aims to take over the heavy
management and simplify your life. Ohara leverage the docker technology
to run all process in containers. If you are able to use k8s, ohara is
good at deploying all containers via k8s. If you are too afraid to touch
k8s, Ohara is doable to be based on ssh connection to control all
containers.

Ohara automatically configure all clusters for you. Of course, you have
the freedom to overwrite any settings. see section
:ref:`zookeeper <rest-zookeeper>`, :ref:`broker <rest-broker>` and
:ref:`worker <rest-worker>` to see more details.

In order to provide a great experience in exercising containers, ohara
pre-builds a lot of docker images with custom scripts. Of course, Ohara
APIs allow you to choose other image instead of ohara official images.
However, it works only if the images you pick up are compatible to ohara
command. see :ref:`here <docker>` for more details. Also, all official
images are hosted by `docker hub <https://cloud.docker.com/u/oharastream/repository/list>`__

Version
-------

We all love to see the version of software, right? Ohara provide a API
to display the details of version. It includes following information.

1. version (**string**) — version of configurator
2. branch (**string**) from which ohara repo branch
3. user (**string**) — builder of configurator
4. revision (**string**) — latest commit of configurator
5. date (**string**) — build date of configurator


get the version of ohara
~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/info*

**Example Response**

.. code-block:: json

   {
     "versionInfo": {
       "version": "$|version|",
       "branch": "master",
       "user": "Chia-Ping Tsai",
       "revision": "9af9578041f069a9a452c7fda5f7ed7217c0deea",
       "date": "2019-03-21 17:55:06"
     }
   }

.. _rest-topic:

Topic
-----

Ohara topic is based on kafka topic. It means the creation of topic on
ohara will invoke a creation of kafka also. Also, the delete to ohara
topic also invoke a delete request to kafka. The common properties in
topic are shown below.

1. group (**string**) — topic group.
2. name (**string**) — topic name
3. brokerClusterName (**option(string)**) — the broker cluster hosting
   this topic (**If you don’t specify the broker cluster in request,
   ohara will try to find a broker cluster for you. And it works only if
   there is only a broker cluster exists in ohara**)
4. numberOfReplications (**option(int)**) — the number of replications
   for this topic (**it is illegal to input the number of replications
   which is larger than the number of broker nodes**)
5. numberOfPartitions (**option(int)**)— the number of partitions for
   this topic
6. configs (**option(object)**) — the custom configs used to create topic
7. state (**option(string)**) — state of a running topic. nothing if the topic is not running.
8. tags (**option(object)**) — the extra description to this
   object

..

   The name must be unique in a broker cluster.

The following information are tagged by ohara.

1. group (**string**) — the group value is always “default” (the default
   value will be changed to be equal to brokerClusterName as the group
   of topic is “broker cluster”)
2. lastModified (**long**) — the last time to update this ftp
   information

store a topic properties
~~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/topics*

1. group (**string**) — topic group. Default group is "default".
2. name (**string**) — topic name
3. brokerClusterName (**option(string)**) — the broker cluster hosting
   this topic (**If you don’t specify the broker cluster in request,
   ohara will try to find a broker cluster for you. And it works only if
   there is only a broker cluster exists in ohara**)
4. numberOfReplications (**option(int)**) — the number of replications
   for this topic (**it is illegal to input the number of replications
   which is larger than the number of broker nodes**)
5. numberOfPartitions (**option(int)**)— the number of partitions for
   this topic
6. configs (**option(object)**) — the custom configs used to create topic
7. state (**option(string)**) — state of a running topic. nothing if the topic is not running.
8. tags (**option(object)**) — the extra description to this
   object

.. note::
  1. the name you pass to ohara is used to build topic on kafka, and it is restricted by kafka ([a-zA-Z0-9\._\-])
  2. the ignored fields will be auto-completed by Ohara Configurator. Also, you could update/replace it by UPDATE request later.
  3. this API does NOT create a topic on broker cluster. Instead, you should sent START request to run a topic on broker cluster actually

**Example Request**

.. code-block:: json

   {
     "name": "topic0",
     "numberOfReplications": 1,
     "numberOfPartitions": 1
   }


**Example Response**

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

..

   The topic, which is just created, does not have any metrics.

update a topic properties
~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/topics/${name}?group=${group}*

1. numberOfPartitions (**int**) — the number of partitions for this
   topic (**it is illegal to decrease the number**)
2. tags (**array(string)**) — the extra description to this object

Both number of replications and configs are unmodifiable. An error response is produced if the update request tries to
update them.

**Example Request**

.. code-block:: json

   {
     "numberOfPartitions": 3
   }


**Example Response**

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
~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/topics*

**Example Response**

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
~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/topics/${name}?group=${group}*

**Example Response**

  ::

     204 NoContent

.. note::
  It is ok to delete an nonexistent topic, and the response is 204 NoContent.


get a topic properties
~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/topics/${name}*

**Example Response**

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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/topics/${name}/start*


**Example Response**

  ::

     202 Accepted

stop a topic from remote broker cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/topics/${name}/stop*

.. note::
  the topic will lose all data after stopping.

**Example Response**

  ::

     202 Accepted


FTP Connection Information
--------------------------

You can store the ftp information in ohara if the data is used
frequently. Currently, all data are stored by text. The storable
information is shown below.

1. name (**string**) — name of this ftp information
2. hostname (**string**) — ftp server hostname
3. port (**int**) — ftp server port
4. user (**string**) — account of ftp server
5. password (**string**) — password of ftp server
6. tags (**object**) — the extra description to this object
7. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

The following information are tagged by ohara.

1. lastModified (**long**) — the last time to update this ftp
   information


store a ftp information
~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/ftp*

1. name (**string**) — name of this ftp information
2. hostname (**string**) — ftp server hostname
3. port (**int**) — ftp server port
4. user (**string**) — account of ftp server
5. password (**string**) — password of ftp server
6. tags (**object**) — the extra description to this object
7. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

.. note::
   the string value can’t be empty or null. the port should be small
   than 65535 and larger than zero. the default value of group is
   “default”

**Example Request**

  .. code-block:: json

     {
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "tags": ["a"]
     }

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": ["a"]
     }


update a ftp information
~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/ftp/$name?group=$group*

1. name (**string**) — name of this ftp information
2. hostname (**option(string)**) — ftp server hostname
3. port (**option(int)**) — ftp server port
4. user (**option(string)**) — account of ftp server
5. password (**option(string)**) — password of ftp server
6. tags (**option(object)**) — the extra description to this
   object
7. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

.. note::
   the string value can’t be empty or null. the port should be small
   than 65535 and larger than zero.

**Example Request**

  .. code-block:: json

     {
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

  .. note::
     Noted, this APIs will create an new ftp object if the input name is
     not associated to an existent object. the default value of group is
     “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": {}
     }


list all ftp information stored in ohara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/ftp*

**Example Response**

  .. code-block:: json

     [
       {
         "group": "default",
         "name": "ftp0",
         "hostname": "node00",
         "port": 22,
         "user": "abc",
         "password": "pwd",
         "lastModified": 1553498552595,
         "tags": {}
       }
     ]


delete a ftp information
~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/ftp/$name?group=$group*

1. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

  .. note::
     the default value of group is “default”

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent ftp information, and
     the response is 204 NoContent.

get a ftp information
~~~~~~~~~~~~~~~~~~~~~

*GET /v0/ftp/$name?group=$group*

   the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": {}
     }

HDFS Connection Information
---------------------------

Ohara supports to store the simple hdfs information which is running on
single namenode without security configuration.

1. name (**string**) — name of this hdfs information.
2. uri (**string**) — hdfs connection information. The form looks like
   “hdfs://namenode:9999/”
3. tags (**object**) — the extra description to this object
4. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

The following information are tagged by ohara.

1. lastModified (**long**) — the last time to update this hdfs
   information

store a hdfs information
~~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/hdfs*

1. name (**string**) — name of this hdfs information.
2. uri (**string**) — hdfs connection information. The form looks like
   “hdfs://namenode:9999/”
3. tags (**object**) — the extra description to this object
4. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

**Example Request**

  .. code-block:: json

     {
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999"
     }

  .. note::
     the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }

update a hdfs information
~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/hdfs/$name?group=$group*

1. name (**string**) — name of this hdfs information.
2. uri (**option(string)**) — hdfs connection information. The form
   looks like "hdfs://namenode:9999/"
3. tags (**object**) — the extra description to this object
4. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

**Example Request**

.. code-block:: json

   {
     "group": "default",
     "name": "hdfs0",
     "uri": "hdfs://namenode:9999"
   }

.. note::
   This API creates an new object if input name does not exist.
   the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }


list all hdfs information stored in ohara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/hdfs*

**Example Response**

  .. code-block:: json

     [
       {
         "group": "default",
         "name": "hdfs0",
         "uri": "hdfs://namenode:9999",
         "lastModified": 1553498552595,
         "tags": {}
       }
     ]


delete a hdfs information
~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/hdfs/$name?group=$group*

1. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

..

   the default value of group is “default”

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent hdfs information, and
     the response is 204 NoContent.


get a hdfs information
~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/hdfs/$name?group=$group*

1. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

  .. note::

     the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }


JDBC Connection Information
---------------------------

Database is a common data source in our world. Ohara also supports to
link database to be a part of streaming, so there are also APIs which
help us to store related information used to connect database. Given
that we are in java world, the jdbc is only supported now. The storable
information is shown below.

1. name (**string**) — name of this jdbc information.
2. url (**string**) — jdbc connection information. format:
   jdbc:$database://$serverName\$instanceName:$portNumber
3. user (**string**) — the account which has permission to access
   database
4. password (**string**) — password of account. It is stored as text in
   ohara
5. tags (**object**) — the extra description to this object
6. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

The following information are tagged by ohara.

1. lastModified (**long**) — the last time to update this jdbc
   information

store a jdbc information
~~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/jdbc*

1. name (**string**) — name of this jdbc information.
2. url (**string**) — jdbc connection information. format:
   jdbc:$database://$serverName\$instanceName:$portNumber
3. user (**string**) — the account which has permission to access
   database
4. password (**string**) — password of account. It is stored as text in
   ohara
5. tags (**object**) — the extra description to this object
6. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

**Example Request**

  .. code-block:: json

     {
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "user": "user",
       "password": "aaa"
     }

  ..

     the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "lastModified": 1540967970407,
       "user": "user",
       "password": "aaa",
       "tags": {}
     }

update a jdbc information
~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/jdbc/$name?group=$group*

1. name (**string**) — name of this jdbc information.
2. url (**option(string)**) — jdbc connection information. format:
   jdbc:$database://$serverName\$instanceName:$portNumber
3. user (**option(string)**) — the account which has permission to
   access database
4. password (**option(string)**) — password of account. It is stored as
   text in ohara
5. tags (**object**) — the extra description to this object
6. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

**Example Request**

  .. code-block:: json

     {
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "user": "user",
       "password": "aaa"
     }

  .. note::
     An new object will be created if the input name is not
     associated to an existent object. the default value of group is
     “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "lastModified": 1540967970407,
       "user": "user",
       "password": "aaa",
       "tags": {}
     }


list all jdbc information stored in ohara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/jdbc*

**Example Response**

  .. code-block:: json

     [
       {
         "group": "default",
         "name": "jdbc_name",
         "url": "jdbc:mysql",
         "lastModified": 1540967970407,
         "user": "user",
         "password": "aaa",
         "tags": {}
       }
     ]


delete a jdbc information
~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/jdbc/$name?group=$group*

1. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

  .. note::
     the default value of group is “default”

**Example Response**

  ::

     204 NoContent

  ..

     It is ok to delete an jar from an nonexistent jdbc information, and
     the response is 204 NoContent.


get a jdbc information
~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/jdbc/$name?group=$group*

   the default value of group is “default”

**Example Response**

  .. code-block:: json

     {
       "group": "default",
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "lastModified": 1540967970407,
       "user": "user",
       "password": "aaa",
       "tags": {}
     }

.. _rest-connector:

Connector
---------

Connector is core of application in ohara :ref:`pipeline <rest-pipeline>`.
Connector has two type - source and sink. Source connector pulls data
from another system and then push to topic. By contrast, Sink connector
pulls data from topic and then push to another system. In order to use
connector in :ref:`pipeline <rest-pipeline>`, you have to set up a connector
settings in ohara and then add it to :ref:`pipeline <rest-pipeline>`. Of
course, the connector settings must belong to a existent connector in
target worker cluster. By default, worker cluster hosts only the
official connectors. If you have more custom requirement for connector,
please follow :ref:`custom connector guideline <connector>` to
write your connector.

Apart from custom settings, common settings are required by all
connectors. The common settings are shown below.

#. connector.group (**string**) — the value of group is always "default"
   (and it will be replaced by workerClusterName...see :ohara-issue:`1734`
#. connector.name (**string**) — the name of this connector
#. connector.class (**class**) — class name of connector implementation
#. topics(**array(string)**) — the source topics or target topics for this connector
#. columns (**array(object)**) — the schema of data for this connector

  - columns[i].name (**string**) — origin name of column
  - columns[i].newName (**string**) — new name of column
  - columns[i].dataType (**string**) — the type used to convert data
  - columns[i].order (**int**) — the order of this column

5. numberOfTasks (**int**) — the number of tasks
#. workerClusterName (**string**) — target worker cluster
#. tags (**object**) — the extra description to this object

The following information are updated by ohara.

#. group (**string**) — connector’s group
#. name (**string**) — connector’s name
#. lastModified (**long**) — the last time to update this connector
#. state (**option(string)**) — the state of a started connector. If the connector is not started, you won’t see this field
#. error (**option(string)**) — the error message from a failed connector. If the connector is fine or un-started, you won’t get this field.
#. :ref:`metrics <connector-metrics>` (**object**) — the metrics from a running connector

  - meters (**array(object)**) — the metrics in meter type

    - meters[i].value (**double**) — the number stored in meter
    - meters[i].unit (**string**) — unit for value
    - meters[i].document (**string**) — document of this meter
    - meters[i].queryTime (**long**) — the time of query metrics from remote machine
    - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

The settings from request, BTW, is a individual item in response. Hence,
you will observe the following response after you store the settings with connector.class.

  .. code-block:: json

     {
       "settings": {
         "connector.class": "abc"
       }
     }

.. _rest-connector-create-settings:

create the settings of connector
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/connectors*

It is ok to lack some common settings when creating settings for a
connector. However, it is illegal to start a connector with incomplete
settings. For example, storing the settings consisting of only
**connector.name** is ok. But stating a connector with above incomplete
settings will introduce a error.

**Example Request**

  .. code-block:: json

     {
       "name": "jdbc_name",
       "connector.class": "com.island.ohara.connector.ftp.FtpSource"
     }

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "group": "default",
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource",
         "tags": {}
       },
       "metrics": {
         "meters": []
       }
     }


update the settings of connector
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}?group=${group}*

.. note::
  you cannot delete a non-stopped connector.

**Example Request**

  .. code-block:: json

     {
       "name": "jdbc_name",
       "connector.class": "com.island.ohara.connector.ftp.FtpSource"
     }

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "group": "default",
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource",
         "tags": {}
       },
       "metrics": {
         "meters": []
       }
     }


list information of all connectors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/connectors*

**Eample Response**

  .. code-block:: json

     [
       {
         "lastModified": 1540967970407,
         "group": "default",
         "name": "jdbc_name",
         "settings": {
           "connector.name": "jdbc_name",
           "connector.class": "com.island.ohara.connector.ftp.FtpSource",
           "tags": {}
         },
         "metrics": {
           "meters": []
         }
       }
     ]

.. _rest-connectors-delete:

delete a connector
~~~~~~~~~~~~~~~~~~

*DELETE /v0/connectors/${name}?group=${group}*

Deleting the settings used by a running connector is not allowed. You
should :ref:`stop <rest-stop-connector>` connector before deleting it.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent connector or a running
     connector, and the response is 204 NoContent.

.. _rest-connectors-get-info:

get information of connector
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/connectors/${name}?group=${group}*

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "group": "default",
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource",
         "tags": {}
       },
       "metrics": {
         "meters": []
       }
     }

start a connector
~~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}/start?group=${group}*

Ohara will send a start request to specific worker cluster to start the
connector with stored settings, and then make a response to called. The
connector is executed async so the connector may be still in starting
after you retrieve the response. You can send
:ref:`GET request <rest-connectors-get-info>` to see the state of
connector. This request is idempotent so it is safe to retry this
command repeatedly.

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource",
         "tags": {}
       },
       "state": "RUNNING",
       "metrics": {
         "meters": [
           {
             "value": 1234,
             "unit": "rows",
             "document": "number of processed rows",
             "queryTime": 1563429505055,
             "startTime": 1563429590505
           }
         ]
       }
     }

.. _rest-stop-connector:

stop a connector
~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}/stop?group=${group}*

Ohara will send a stop request to specific worker cluster to stop the
connector. The stopped connector will be removed from worker cluster.
The settings of connector is still kept by ohara so you can start the
connector with same settings again in the future. If you want to delete
the connector totally, you should stop the connector and then
:ref:`delete <rest-connectors-delete>` it. This request is idempotent so it is
safe to send this request repeatedly.

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource"
       },
       "metrics": {
         "meters": []
       }
     }


pause a connector
~~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}/pause?group=${group}*

Pausing a connector is to disable connector to pull/push data from/to
source/sink. The connector is still alive in kafka. This request is
idempotent so it is safe to send this request repeatedly.

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource"
       },
       "state": "PAUSED",
       "metrics": {
         "meters": [
           {
             "value": 1234,
             "unit": "rows",
             "document": "number of processed rows",
             "queryTime": 15623429590505,
             "startTime": 15623429590505
           }
         ]
       }
     }

resume a connector
~~~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}/resume?group=${group}*

Resuming a connector is to enable connector to pull/push data from/to
source/sink. This request is idempotent so it is safe to retry this
command repeatedly.

**Example Response**

  .. code-block:: json

     {
       "lastModified": 1540967970407,
       "name": "jdbc_name",
       "settings": {
         "connector.name": "jdbc_name",
         "connector.class": "com.island.ohara.connector.ftp.FtpSource"
       },
       "state": "RUNNING",
       "metrics": {
         "meters": [
           {
             "value": 1234,
             "unit": "rows",
             "document": "number of processed rows",
             "queryTime": 1563429509054,
             "startTime": 1563429590505
           }
         ]
       }
     }

.. _rest-pipeline:

Pipeline
--------

Pipeline APIs are born of ohara-manager which needs a way to store the
relationship of components in streaming. The relationship in pipeline is
made up of multi **flows**. Each **flow** describe a **from** and multi **to**\s. For example,
you have a :ref:`topic <rest-topic>` as source and a :ref:`connector <rest-connector>`
as consumer, so you can describe the
relationship via following flow.

.. code-block:: json

   {
     "flows": [
       {
         "from": "topic's name",
         "to": ["connector's name"]
       }
     ]
   }

The objects grouped by pipeline should be existent. Otherwise, pipeline
will ignore them in generating object abstracts.

The objects grouped by pipeline don’t need to located on the same
cluster hierarchy. Grouping a topic, which is placed at broker_0, and a
topic, which is located at broker_1, is valid. However, the object based
on a dead cluster will get an abstract with error state.

The properties used in generating pipeline are shown below.

1. group (**string**) — pipeline’s name
2. name (**string**) — pipeline’s name
3. flows (**array(object)**) — the relationship between objects

  - flows[i].from (**object**) — the endpoint of source

    - flows[i].from.group — the group of source
    - flows[i].from.name — the name of source

  - flows[i].to (**array(object)**) — the endpoint of sinks

    - flows[i].to[j].group — the group of sink[j]
    - flows[i].to[j].name — the name of sink[j]

4. tags (**object**) — the extra description to this object


Following information are written by ohara.

1. lastModified (**long**) — the last time to update this pipeline
2. objects (**array(object)**) — the abstract of all objects mentioned by pipeline

    - objects[i].name (**string**) — object’s name
    - objects[i].kind (**string**) — the type of this object. for instance, :ref:`topic <rest-topic>`,
        :ref:`connector <rest-connector>`, and :ref:`streamapp <rest-streamapp>`
    - objects[i].className (**string**) — object’s implementation. Normally, it shows the full name of
        a java class
    - objects[i].state (**option(string)**) — the state of object. If the object can’t have state
        (eg, :ref:`topic <rest-topic>`), you won’t see this field
    - objects[i].error (**option(string)**) — the error message of this object
    - objects[i].lastModified (**long**) — the last time to update this object
    - :ref:`metrics <connector-metrics>` (**object**) — the metrics from this object.
        Not all objects in pipeline have metrics!
    - meters (**array(object)**) — the metrics in meter type
    - meters[i].value (**double**) — the number stored in meter
    - meters[i].unit (**string**) — unit for value
    - meters[i].document (**string**) — document of this meter
    - meters[i].queryTime (**long**) — the time of query metrics from remote machine
    - meters[i].startTime (**option(long)**) — the time of record generated in remote machine


create a pipeline
~~~~~~~~~~~~~~~~~

*POST /v0/pipelines*

The following example creates a pipeline with a :ref:`topic <rest-topic>` and
:ref:`connector <rest-connector>`. The :ref:`topic <rest-topic>` is created on
:ref:`broker cluster <rest-broker>` but the :ref:`connector <rest-connector>` isn’t. Hence,
the response from server shows that it fails to find the status of the
:ref:`connector <rest-connector>`. That is to say, it is ok to add un-running
:ref:`connector <rest-connector>` to pipeline.

**Example Request 1**

  .. code-block:: json

     {
       "name": "pipeline0",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-aaa",
           "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
         }
       ]
     }

**Example Response 1**

  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }

  .. note::
    Don’t worry about creating a pipeline with incomplete flows. It is ok to
    add a flow with only **from**. The following example creates a pipeline
    with only a object and leave empty in **to** field.

**Example Request 1**

  .. code-block:: json

     {
       "name": "pipeline1",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": []
         }
       ]
     }

**Example Response 1**

  .. code-block:: json

     {
       "name": "pipeline1",
       "lastModified": 1554952500972,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": []
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         }
       ],
       "tags": {}
     }


update a pipeline
~~~~~~~~~~~~~~~~~

*PUT /v0/pipelines/$name*

**Example Request**

  .. code-block:: json

     {
       "name": "pipeline0",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-aaa",
           "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
         }
       ]
     }

  .. note::
    This API creates an new pipeline for you if the input name
    does not exist!

**Example Response**

  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }


list all pipelines
~~~~~~~~~~~~~~~~~~

*GET /v0/pipelines*

Listing all pipelines is a expensive operation as it invokes a iteration
to all objects stored in pipeline. The loop will do a lot of checks and
fetch status, metrics and log from backend clusters. If you have the
name of pipeline, please use :ref:`GET <rest-pipeline-get>` to fetch details
of **single** pipeline.

**Example Response**

  .. code-block:: json

     [
       {
         "name": "pipeline0",
         "lastModified": 1554950999668,
         "flows": [
           {
             "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
             "to": [
               "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
             ]
           }
         ],
         "objects": [
           {
             "group": "default",
             "name": "topic0",
             "lastModified": 1554950034608,
             "metrics": {
               "meters": []
             },
             "kind": "topic",
             "tags": {}
           },
           {
             "group": "default",
             "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
             "lastModified": 1554950058696,
             "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
             "metrics": {
               "meters": []
             },
             "kind": "connector",
             "tags": {}
           }
         ],
         "tags": {}
       }
     ]


delete a pipeline
~~~~~~~~~~~~~~~~~

*DELETE /v0/pipelines/$name*

Deleting a pipeline does not delete the objects related to the pipeline.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an an nonexistent pipeline, and the response is
     204 NoContent. However, it is illegal to remove a pipeline having any
     running objects

.. _rest-pipeline-get:

get a pipeline
~~~~~~~~~~~~~~

*GET /v0/pipelines/$name*

**Example Response**

  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }

.. _rest-node:

Node
----

Node is the basic unit of running service. It can be either physical
machine or vm. In section :ref:`zookeeper <rest-zookeeper>`,
:ref:`Broker <rest-broker>` and :ref:`Worker <rest-worker>`, you will see many
requests demanding you to fill the node name to build the services.
Currently, ohara requires the node added to ohara should pre-install
following services.

#. docker (18.09+)
#. ssh server
#. k8s (only if you want to k8s to host containers)
#. official ohara images

  - `oharastream/zookeeper <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/zookeeper>`__
  - `oharastream/broker <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/broker>`__
  - `oharastream/connect-worker <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/connect-worker>`__
  - `oharastream/streamapp <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/streamapp>`__

The version (tag) depends on which ohara you used. It would be better to
use the same version to ohara. For example, the version of ohara
configurator you are running is 0.4, then the official images you should
download is oharastream/xx:0.4.

The properties used in describing a node are shown below.

#. hostname (**string**) — hostname of node.
    This hostname must be available on you DNS.
    It will cause a lot of troubles if Ohara Configurator is unable to
    connect to remote node via this hostname.
#. port (**int**) — ssh port of node
#. user (**string**) — ssh account
#. password (**string**) — ssh password
#. tags (**object**) — the extra description to this object
#. validationReport (**object**) — last validation result.
    This information is attached by Ohara Configurator after you request the :ref:`validation <rest-validation>`

  - validationReport.hostname (**string**) — the host which is in charge of validating node
  - validationReport.message (**string**) — the report
  - validationReport.pass (**boolean**) — true if the arguments is able to be connected
  - validationReport.lastModified (**long**) — the time to execute this validation

.. note::
   ohara use above information to login node to manage the containers.
   Please make sure the account has permission to operate docker (and
   k8s service) without sudo.

The following information are tagged by ohara.

1. lastModified (**long**) — the last time to update this node


store a node
~~~~~~~~~~~~

*POST /v0/nodes*

1. hostname (**string**) — hostname of node
2. port (**int**) — ssh port of node
3. user (**string**) — ssh account
4. password (**string**) — ssh password

**Example Request**

  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

**Example Response**

  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": {}
     }


update a node
~~~~~~~~~~~~~

*PUT /v0/nodes/${name}*

1. hostname (**string**) — hostname of node
2. port (**int**) — ssh port of node
3. user (**string**) — ssh account
4. password (**string**) — ssh password

**Example Request**

  .. code-block:: json

     {
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

  .. note::
     An new node will be created if your input name does not exist

  .. note::
     the update request will clear the validation report attached to this node

**Example Response**

  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": {}
     }


list all nodes stored in ohara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/nodes*

**Example Response**

  .. code-block:: json

     [
       {
         "hostname": "node00",
         "port": 22,
         "user": "abc",
         "password": "pwd",
         "lastModified": 1553498552595,
         "tags": {}
       }
     ]


delete a node
~~~~~~~~~~~~~

*DELETE /v0/nodes/${name}*

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an an nonexistent pipeline, and the response is
     204 NoContent. However, it is disallowed to remove a node which is
     running service. If you do want to delete the node from ohara, please
     stop all services from the node.

get a node
~~~~~~~~~~

*GET /v0/nodes/${name}*

**Example Response**

  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "lastModified": 1553498552595,
       "tags": {}
     }

.. _rest-zookeeper:

Zookeeper
---------

`Zookeeper <https://zookeeper.apache.org>`__ service is the base of all
other services. It is also the fist service you should set up. At the
beginning, you can deploy zookeeper cluster in single node. However, it
may be unstable since single node can’t guarantee the data durability
when node crash. In production you should set up zookeeper cluster on 3
nodes at least.

Zookeeper service has many configs which make you spend a lot of time to
read and set. Ohara provides default values to all configs but open a
room to enable you to overwrite somethings you do care.

#. name (**string**) — cluster name
#. imageName (**string**) — docker image
#. clientPort (**int**) — broker client port.
#. electionPort (**int**) — used to select the zk node leader
#. peerPort (**int**) — port used by internal communication
#. nodeNames (**array(string)**) — the nodes running the zookeeper process
#. deadNodes (**array(string)**) — the nodes that have failed containers of zookeeper
#. tags (**object**) — the user defined parameters
#. state (**option(string)**) — only started/failed zookeeper has state (RUNNING or DEAD)
#. error (**option(string)**) — the error message from a failed zookeeper. If zookeeper is fine or un-started,
   you won’t get this field.
#. lastModified (**long**) — last modified this jar time


.. _rest-create-zookeeper-properties:

create a zookeeper properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/zookeepers*

#. name (**string**) — cluster name
#. imageName (**string**) — docker image
#. clientPort (**int**) — broker client port.
#. electionPort (**int**) — used to select the zk node leader
#. peerPort (**int**) — port used by internal communication
#. nodeNames (**array(string)**) — the nodes running the zookeeper process
#. tags (**object**) — the user defined parameters

**Example Request**

  .. code-block:: json

     {
       "name": "zk00",
       "imageName": "oharastream/zookeeper:$|version|",
       "clientPort": 12345,
       "peerPort": 12346,
       "electionPort": 12347,
       "nodeNames": [
         "node00"
       ],
       "tags": {}
     }

**Example Response**

  .. code-block:: json

     {
       "name": "zk00",
       "imageName": "oharastream/zookeeper:$|version|",
       "clientPort": 12345,
       "peerPort": 12346,
       "electionPort": 12347,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": [],
       "tags": {},
       "lastModified": 1563158986411
     }

  As mentioned before, ohara provides default to most settings. You can
  just input nodeNames to run a zookeeper cluster.

**Example Request**

  .. code-block:: json

     {
       "nodeNames": [
         "node00"
       ]
     }

  .. note::
    All ports have default value so you can ignore them when creating
    zookeeper cluster. However, the port conflict detect does not allow
    you to reuse port on different purpose (a dangerous behavior, right?).

**Example Response**

  .. code-block:: json

     {
       "name": "zk00",
       "electionPort": 3888,
       "imageName": "oharastream/zookeeper:$|version|",
       "clientPort": 2181,
       "peerPort": 2888,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": [],
       "tags": {},
       "lastModified": 1563158986411
     }


list all zookeeper clusters
~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/zookeepers*

**Example Response**

  .. code-block:: json

     [
       {
         "name": "zk00",
         "electionPort": 12347,
         "imageName": "oharastream/zookeeper:$|version|",
         "clientPort": 12345,
         "peerPort": 12346,
         "nodeNames": [
           "node00"
         ],
         "deadNodes": [],
         "tags": {},
         "state": "RUNNING"
       }
     ]


delete a zookeeper properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/zookeepers/$name*

You cannot delete properties of an non-stopped zookeeper cluster.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent zookeeper cluster, and the response is 204 NoContent.

.. _rest-zookeeper-get:

get a zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/zookeepers/$name*

Get zookeeper information by name. This API could fetch all information
of a zookeeper (include state)

**Example Response**

  .. code-block:: json

     {
       "name": "zk00",
       "electionPort": 12347,
       "imageName": "oharastream/zookeeper:$|version|",
       "clientPort": 12345,
       "peerPort": 12346,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": [],
       "tags": {},
       "state": "RUNNING"
     }


start a zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/zookeepers/$name/start*

**Example Response**

  ::

    202 Accepted

  .. note::
    You should use :ref:`Get zookeeper cluster <rest-zookeeper-get>` to fetch up-to-date status

stop a zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~

Gracefully stopping a running zookeeper cluster. It is disallowed to
stop a zookeeper cluster used by a running :ref:`broker cluster <rest-broker>`.

*PUT /v0/zookeepers/$name/stop[?force=true]*

**Query Parameters**

1. force (**boolean**) — true if you don’t want to wait the graceful shutdown
    (it can save your time but may damage your data).

**Example Response**

  ::

    202 Accepted

  .. note::
    You should use :ref:`Get zookeeper cluster <rest-zookeeper-get>` to fetch up-to-date status


delete a node from a running zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unfortunately, it is a litter dangerous to remove a node from a running
zookeeper cluster so we don’t support it yet.


add a node to a running zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unfortunately, it is a litter hard to add a node to a running zookeeper
cluster so we don’t support it yet.

.. _rest-broker:

Broker
------

`Broker <https://kafka.apache.org/intro>`__ is core of data transmission
in ohara. The topic, which is a part our data lake, is hosted by broker
cluster. The number of brokers impacts the performance of transferring
data and data durability. But it is ok to setup broker cluster in single
node when testing. As with :ref:`zookeeper <rest-zookeeper>`, broker has many
configs also. Ohara still provide default to most configs and then
enable user to overwrite them.

Broker is based on :ref:`zookeeper <rest-zookeeper>`, hence you have to create
zookeeper cluster first. Noted that a zookeeper cluster can be used by
only a broker cluster. It will fail if you try to multi broker cluster
on same zookeeper cluster.

The properties which can be set by user are shown below.

#. name (**string**) — cluster name
#. imageName (**string**) — docker image
#. clientPort (**int**) — broker client port
#. exporterPort (**int**) — port used by internal communication
#. jmxPort (**int**) — port used by jmx service
#. zookeeperClusterName (**String**) — name of zookeeper cluster used to store metadata of broker cluster
#. nodeNames (**array(string)**) — the nodes running the broker process
#. deadNodes (**array(string)**) — the nodes that have failed containers of broker
#. tags (**object**) — the user defined parameters
#. state (**option(string)**) — only started/failed broker has state (RUNNING or DEAD)
#. error (**option(string)**) — the error message from a failed broker. If broker is fine or un-started, you won't get this field.
#. lastModified (**long**) — last modified this jar time

.. _rest-create-brokercluster:

create a broker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/brokers*

#. name (**string**) — cluster name
#. imageName (**string**) — docker image
#. clientPort (**int**) — broker client port.
#. exporterPort (**int**) — port used by internal communication
#. jmxPort (**int**) — port used by jmx service
#. zookeeperClusterName (**option(string)**) — name of zookeeper cluster used to store metadata of broker cluster. default will find a zookeeper for you
#. nodeNames (**array(string)**) — the nodes running the broker process
#. tags(**object**) — the user defined parameters

**Example Request**

  .. code-block:: json

     {
       "name": "bk00",
       "imageName": "oharastream/broker:$|version|",
       "zookeeperClusterName": "zk00",
       "clientPort": 12345,
       "exporterPort": 12346,
       "jmxPort": 12347,
       "nodeNames": [
         "node00"
       ],
       "tags": {}
     }

**Example Response**

  .. code-block:: json

     {
       "name": "bk00",
       "zookeeperClusterName": "zk00",
       "imageName": "oharastream/broker:$|version|",
       "exporterPort": 12346,
       "clientPort": 12345,
       "jmxPort": 12347,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": [],
       "tags": {},
       "lastModified": 1563158986411
     }

  As mentioned before, ohara provides default to most settings. You can
  just input name and nodeNames to run a broker cluster.

**Example Request**

  .. code-block:: json

     {
       "name": "bk00",
       "nodeNames": [
         "node00"
       ]
     }

  .. note::
    As you don’t input the zookeeper cluster name, Ohara will try to pick
    up a zookeeper cluster for you. If the number of zookeeper cluster
    host by ohara is only one, ohara do deploy broker cluster on the
    zookeeper cluster. Otherwise, ohara will say that it can’t match a
    zookeeper cluster for you. All ports have default value so you can
    ignore them when creating zookeeper cluster. However, the port
    conflict detect does not allow you to reuse port on different purpose
    (a dangerous behavior, right?).

**Example Response**

  .. code-block:: json

     {
       "name": "bk00",
       "zookeeperClusterName": "zk00",
       "imageName": "oharastream/broker:$|version|",
       "exporterPort": 7071,
       "clientPort": 9092,
       "jmxPort": 9093,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": []
     }

list all broker clusters
~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/brokers*

**Example Response**

  .. code-block:: json

     [
       {
         "name": "bk00",
         "zookeeperClusterName": "zk00",
         "imageName": "oharastream/broker:$|version|",
         "exporterPort": 7071,
         "clientPort": 9092,
         "jmxPort": 9093,
         "nodeNames": [
           "node00"
         ],
         "deadNodes": [],
         "tags": {},
         "state": "RUNNING"
       }
     ]


delete a broker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/brokers/$name*

You cannot delete properties of an non-stopped broker cluster.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent broker cluster, and the response is
     204 NoContent.

.. _rest-broker-get:

get a broker cluster
~~~~~~~~~~~~~~~~~~~~

*GET /v0/brokers/$name*

**Example Response**

  .. code-block:: json

     {
       "name": "bk00",
       "zookeeperClusterName": "zk00",
       "imageName": "oharastream/broker:$|version|",
       "exporterPort": 7071,
       "clientPort": 9092,
       "jmxPort": 9093,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": [],
       "tags": {},
       "state": "RUNNING"
     }

start a broker cluster
~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/brokers/$name/start*

**Example Response**

  ::

    202 Accepted

  .. note::
    You should use :ref:`Get broker cluster <rest-broker-get>` to fetch up-to-date status

stop a broker cluster
~~~~~~~~~~~~~~~~~~~~~

Gracefully stopping a running broker cluster. It is disallowed to
stop a broker cluster used by a running :ref:`worker cluster <rest-worker>`.

*PUT /v0/brokers/$name/stop[?force=true]*

**Query Parameters**

1. force (**boolean**) — true if you don’t want to wait the graceful shutdown
    (it can save your time but may damage your data).

**Example Response**

  ::

    202 Accepted

  .. note::
    You should use :ref:`Get broker cluster <rest-broker-get>` to fetch up-to-date status

add a new node to a running broker cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/brokers/$name/$nodeName*

If you want to extend a running broker cluster, you can add a node to
share the heavy loading of a running broker cluster. However, the
balance is not triggered at once.

**Example Response**

  .. code-block:: json

     {
       "name": "bk00",
       "zookeeperClusterName": "zk00",
       "imageName": "oharastream/broker:$|version|",
       "exporterPort": 7071,
       "clientPort": 9092,
       "jmxPort": 9093,
       "nodeNames": [
         "node01",
         "node00"
       ],
       "deadNodes": []
     }

  .. note::
    Although it's a rare case, you should not use the "API keyword" as the nodeName.
    For example, the following APIs are invalid and will trigger different behavior!

    - /v0/brokers/$name/start
    - /v0/brokers/$name/stop

remove a node from a running broker cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/brokers/$name/$nodeName*

If your budget is limited, you can decrease the number of nodes running
broker cluster. BUT, removing a node from a running broker cluster
invoke a lot of data move. The loading may burn out the remaining nodes.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent broker node, and the response is
     204 NoContent.

