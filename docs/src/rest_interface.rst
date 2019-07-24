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
`zookeeper <#zookeeper>`__, `broker <#broker>`__ and
`worker <#worker>`__ to see more details.

In order to provide a great experience in exercising containers, ohara
pre-builds a lot of docker images with custom scripts. Of course, Ohara
APIs allow you to choose other image instead of ohara official images.
However, it works only if the images you pick up are compatible to ohara
command. see `here <docker.html>`__ for more details. Also, all official
images are hosted by `docker
hub <https://cloud.docker.com/u/oharastream/repository/list>`__

Version
-------

We all love to see the version of software, right? Ohara provide a API
to display the details of version. It includes following information.

1. version (**string**) — version of configurator
2. branch(\ **string**) from which ohara repo branch
3. user(\ **string**) — builder of configurator
4. revision(\ **string**) — latest commit of configurator
5. date(\ **string**) — build date of configurator

get the version of ohara
~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/info*

**Example Response**

.. code-block:: json

   {
     "versionInfo": {
       "version": "0.3-SNAPSHOT",
       "branch": "master",
       "user": "Chia-Ping Tsai",
       "revision": "9af9578041f069a9a452c7fda5f7ed7217c0deea",
       "date": "2019-03-21 17:55:06"
     }
   }

Topic
-----

Ohara topic is based on kafka topic. It means the creation of topic on
ohara will invoke a creation of kafka also. Also, the delete to ohara
topic also invoke a delete request to kafka. The common properties in
topic are shown below.

1. name (**string**) — topic name
2. brokerClusterName (**string**) — the broker cluster hosting this
   topic
3. numberOfReplications (**int**) — the number of replications for this
   topic
4. numberOfPartitions (**int**) — the number of partitions for this
   topic
5. lastModified (**long**) — the last time to update this topic
6. tags (**array(string)**) — the extra description to this object

..

   The name must be unique in a broker cluster.

The following information are tagged by ohara.

1. group (**string**) — the group value is always “default” (the default
   value will be changed to be equal to brokerClusterName as the group
   of topic is “broker cluster”)
2. lastModified (**long**) — the last time to update this ftp
   information

create a topic
~~~~~~~~~~~~~~

*POST /v0/topics*

1. name (**string**) — topic name
2. brokerClusterName (**option(string)**) — the broker cluster hosting
   this topic (**If you don’t specify the broker cluster in request,
   ohara will try to find a broker cluster for you. And it works only if
   there is only a broker cluster exists in ohara**)
3. numberOfReplications (**option(int)**) — the number of replications
   for this topic (**it is illegal to input the number of replications
   which is larger than the number of broker nodes**)
4. numberOfPartitions (**option(int)**)— the number of partitions for
   this topic
5. tags (**option(array(string))**) — the extra description to this
   object

**Example Request**

.. code-block:: json

   {
     "name": "topic0",
     "numberOfReplications": 1,
     "numberOfPartitions": 1
   }

..

   the name you pass to ohara is used to build topic on kafka, and it is
   restricted by kafka ([a-zA-Z0-9\._\-])

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
     "tags": {}
   }

..

   The topic, which is just created, does not have any metrics.

update a topic
~~~~~~~~~~~~~~

*PUT /v0/topics/${name}*

1. numberOfPartitions (**int**) — the number of partitions for this
   topic (**it is illegal to decrease the number**)
2. tags (**array(string)**) — the extra description to this object

**Example Request**

.. code-block:: json

   {
     "numberOfPartitions": 3
   }

..

   You wil get an exception if you try to change the unmodifiable
   attributes!!!

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
     "tags": {}
   }

list all topics
~~~~~~~~~~~~~~~

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
       "tags": {}
     }
   ]

delete a topic
~~~~~~~~~~~~~~

*DELETE /v0/topics/${name}*

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent topic, and the response
     is 204 NoContent.


get a topic
~~~~~~~~~~~

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
       "tags": {}
     }


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

*POST /v0/ftp?group=${group}*

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
6. tags (**option(array(string))**) — the extra description to this
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

*POST /v0/hdfs?group=${group}*

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

a
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

*POST /v0/jdbc?group=$group*

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

Connector
---------

Connector is core of application in ohara `pipeline <#pipeline>`__.
Connector has two type - source and sink. Source connector pulls data
from another system and then push to topic. By contrast, Sink connector
pulls data from topic and then push to another system. In order to use
connector in `pipeline <#pipeline>`__, you have to set up a connector
settings in ohara and then add it to `pipeline <#pipeline>`__. Of
course, the connector settings must belong to a existent connector in
target worker cluster. By default, worker cluster hosts only the
official connectors. If you have more custom requirement for connector,
please follow `custom connector guideline <custom_connector.html>`__ to
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

#. name (**string**) — connector’s name
#. lastModified (**long**) — the last time to update this connector
#. state (**option(string)**) — the state of a started connector. If the connector is not started, you won’t see this field
#. error (**option(string)**) — the error message from a failed connector. If the connector is fine or un-started, you won’t get this field.
#. `metrics <custom_connector.html#metrics>`__ (**object**) — the metrics from a running connector

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

*PUT /v0/connectors/${name}*

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

**Example Response**

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


delete a connector
~~~~~~~~~~~~~~~~~~

*DELETE /v0/connectors/${name}*

Deleting the settings used by a running connector is not allowed. You
should `stop <#stop-a-connector>`__ connector before deleting it.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent connector or a running
     connector, and the response is 204 NoContent.

get information of connector
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/connectors/${name}*

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

*PUT /v0/connectors/${name}/start*

Ohara will send a start request to specific worker cluster to start the
connector with stored settings, and then make a response to called. The
connector is executed async so the connector may be still in starting
after you retrieve the response. You can send `GET
request <#get-information-of-connector>`__ to see the state of
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


stop a connector
~~~~~~~~~~~~~~~~

*PUT /v0/connectors/${name}/stop*

Ohara will send a stop request to specific worker cluster to stop the
connector. The stopped connector will be removed from worker cluster.
The settings of connector is still kept by ohara so you can start the
connector with same settings again in the future. If you want to delete
the connector totally, you should stop the connector and then
`delete <#delete-a-connector>`__ it. This request is idempotent so it is
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

*PUT /v0/connectors/${name}/pause*

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

*PUT /v0/connectors/${name}/resume*

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


Pipeline
--------

Pipeline APIs are born of ohara-manager which needs a way to store the
relationship of components in streaming. The relationship in pipeline is
made up of multi **flows**. Each **flow** describe a **from** and multi **to**\s. For example,
you have a `topic <#topic>`__ as source and a `connector <#connector>`__ as consumer, so you can describe the
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

1. name (**string**) — pipeline’s name
2. flows (**array(object)**) — the relationship between objects
    - flows[i].from (**string**) — the endpoint of source
    - flows[i].to (**array(string)**) — the endpoint of sink
3. tags (**object**) — the extra description to this object


Following information are written by ohara.

1. lastModified (**long**) — the last time to update this pipeline
2. objects (**array(object)**) — the abstract of all objects mentioned by pipeline
    - objects[i].name (**string**) — object’s name
    - objects[i].kind (**string**) — the type of this object. for instance, `topic <#topic>`__,
      `connector <#connector>`__, and `streamapp <#streamapp>`__
    - objects[i].className (**string**) — object’s implementation. Normally, it shows the full name of
      a java class
    - objects[i].state (**option(string)**) — the state of object. If the object can’t have state
      (eg, `topic <#topic>`__), you won’t see this field
    - objects[i].error (**option(string)**) — the error message of this object
    - objects[i].lastModified (**long**) — the last time to update this object
    - `metrics <custom_connector.html#metrics>`__ (**object**) — the metrics from this object.
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

The following example creates a pipeline with a `topic <#topic>`__ and
`connector <#connector>`__. The `topic <#topic>`__ is created on `broker
cluster <#broker>`__ but the `connector <#connector>`__ isn’t. Hence,
the response from server shows that it fails to find the status of the
`connector <#connector>`__. That is to say, it is ok to add un-running
`connector <#connector>`__ to pipeline.

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
name of pipeline, please use `GET <#get-a-pipeline>`__ to fetch details
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

Node
----

Node is the basic unit of running service. It can be either physical
machine or vm. In section `Zookeeper <#zookeeper>`__,
`Broker <#broker>`__ and `Worker <#worker>`__, you will see many
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
    This information is attached by Ohara Configurator after you request the `validation <#validation>`__

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
    You should use `Get zookeeper cluster <#get-a-zookeeper-cluster>`__ to fetch up-to-date status

stop a zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~

Gracefully stopping a running zookeeper cluster. It is disallowed to
stop a zookeeper cluster used by a running `broker cluster <#broker>`__.

*PUT /v0/zookeepers/$name/stop[?force=true]*

**Query Parameters**

1. force (**boolean**) — true if you don’t want to wait the graceful shutdown
    (it can save your time but may damage your data).

**Example Response**

  ::

    202 Accepted

  .. note::
    You should use `Get zookeeper cluster <#get-a-zookeeper-cluster>`__ to fetch up-to-date status


delete a node from a running zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unfortunately, it is a litter dangerous to remove a node from a running
zookeeper cluster so we don’t support it yet.


add a node to a running zookeeper cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unfortunately, it is a litter hard to add a node to a running zookeeper
cluster so we don’t support it yet.


Broker
------

`Broker <https://kafka.apache.org/intro>`__ is core of data transmission
in ohara. The topic, which is a part our data lake, is hosted by broker
cluster. The number of brokers impacts the performance of transferring
data and data durability. But it is ok to setup broker cluster in single
node when testing. As with `Zookeeper <#zookeeper>`__, broker has many
configs also. Ohara still provide default to most configs and then
enable user to overwrite them.

Broker is based on `Zookeeper <#zookeeper>`__, hence you have to create
zookeeper cluster first. Noted that a zookeeper cluster can be used by
only a broker cluster. It will fail if you try to multi broker cluster
on same zookeeper cluster.

The properties which can be set by user are shown below.

1. name (**string**) — cluster name
1. imageName (**string**) — docker image
1. clientPort (**int**) — broker client port
1. exporterPort (**int**) — port used by internal communication
1. jmxPort (**int**) — port used by jmx service
1. zookeeperClusterName (**String**) — name of zookeeper cluster used to store metadata of broker cluster
1. nodeNames (**array(string)**) — the nodes running the broker process
1. deadNodes (**array(string)**) — the nodes that have failed containers of broker


create a broker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/brokers*

1. name (**string**) — cluster name
2. imageName (**string**) — docker image
3. clientPort (**int**) — broker client port.
4. exporterPort (**int**) — port used by internal communication
5. jmxPort (**int**) — port used by jmx service
6. zookeeperClusterName (**String**) — name of zookeeper cluster used to
   store metadata of broker cluster
7. nodeNames (**array(string)**) — the nodes running the broker process

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
       ]
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
       "deadNodes": []
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
         "deadNodes": []
       }
     ]


delete a broker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/brokers/$name*

It is disallowed to remove a broker cluster used by a running `worker cluster <#worker>`__.

**Query Parameters**

1. force (**boolean**) — true if you don’t want to wait the graceful shutdown
   (it can save your time but may damage your data). Other values invoke graceful delete.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent broker cluster, and the response is
     204 NoContent.


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
       "deadNodes": []
     }


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


Worker
------

`Worker <https://kafka.apache.org/intro>`__ is core of running
connectors for ohara. It provides a simple but powerful system to
distribute and execute connectors on different nodes. The performance of
connectors depends on the scale of worker cluster. For example, you can
assign the number of task when creating connector. If there is only 3
nodes within your worker cluster and you specify 6 tasks for your
connector, the tasks of you connectors still be deployed on 3 nodes.
That is to say, the connector can’t get more resources to execute.

Worker is based on `Broker <#broker>`__, hence you have to create broker
cluster first. Noted that a broker cluster can be used by multi worker
clusters. BTW, worker cluster will pre-allocate a lot of topics on
broker cluster, and the pre-created topics CAN’T be reused by different
worker clusters.

The properties which can be set by user are shown below.

#. name (**string**) — cluster name
#. imageName (**string**) — docker image 
#. brokerClusterName (**string**) — broker cluster used to host topics for this worker cluster
#. clientPort (**int**) — worker client port 
#. jmxPort (**int**) — worker jmx port
#. groupId (**string**) — the id of worker stored in broker cluster
#. configTopicName (**string**) — a internal topic used to store connector configuration
#. configTopicReplications (**int**) — number of replications for config topic
#. offsetTopicName (**string**) — a internal topic used to store connector offset
#. offsetTopicPartitions (**int**) — number of partitions for offset topic
#. offsetTopicReplications (**int**) — number of replications for offset topic
#. statusTopicName (**string**) — a internal topic used to store connector status
#. statusTopicPartitions (**int**) — number of partitions for status topic
#. statusTopicReplications (**int**) — number of replications for status topic
#. jarKeys (**array(object)**) — the “primary key” of jars that will be loaded by worker cluster.
   You can require worker cluster to load the jars stored in ohara if you want to run custom connectors
   on the worker cluster. see `Files APIs <#files>`__ for uploading jars to ohara. Noted: the response
   will replace this by `JarInfo <#files>`__.
#. nodeNames (**array(string)**) — the nodes running the worker process
#. deadNodes (**array(string)**) — the nodes that have failed containers of worker

.. note::
   The groupId, configTopicName, offsetTopicName and statusTopicName
   must be unique in broker cluster. Don’t reuse them in same broker
   cluster. Dispatching above unique resources to two worker cluster
   will pollute the data. Of course, ohara do a quick failure for this
   dumb case. However, it is not a quick failure when you are using raw
   kafka rather than ohara. Please double check what you configure!

After building the worker cluster, ohara starts to fetch the details of
available connectors from the worker cluster. The details is the setting
definitions of connector. It shows how to assign the settings to a
connector correctly. The details of connector’s setting definitions can
be retrieved via `GET <#get-a-worker-cluster>`__ or `LIST <#list-all-workers-clusters>`__,
and the JSON representation is shown below.

.. code-block:: json

   {
     "connectors": [
       {
         "className": "xxx",
         "definitions": [
           {
             "reference": "NONE",
             "displayName": "connector.class",
             "internal": false,
             "documentation": "the class name of connector",
             "valueType": "CLASS",
             "tableKeys": [],
             "orderInGroup": 0,
             "key": "connector.class",
             "required": true,
             "defaultValue": null,
             "group": "core",
             "editable": true
           }
         ]
       }
     ]
   }

#. connectors (array(string)) — the available connectors of worker cluster

    - connectors[i].className (**string**) — the class name of available connector
    - connectors[i].definitions (array(object)) — the settings used by this connector

        - connectors[i].definitions[j].displayName (**string**) — the
          readable name of this setting
        - connectors[i].definitions[j].group (**string**) — the group of
          this setting (all core setting are in core group)
        - connectors[i].definitions[j].orderInGroup (**int**) — the order in
          group
        - connectors[i].definitions[j].editable (**boolean**) — true if this
          setting is modifiable
        - connectors[i].definitions[j].key (**string**) — the key of
          configuration
        - connectors[i].definitions[j]. `valueType <#setting-type>`__ (**string**) — the type of value
        - connectors[i].definitions[j].defaultValue (**string**) — the
          default value
        - connectors[i].definitions[j].documentation (**string**) — the
          explanation of this definition
        - connectors[i].definitions[j]. `reference <#setting-reference>`__ (**string**) — works for ohara manager.
          It represents the reference of value.
        - connectors[i].definitions[j].required (**boolean**) — true if
          this setting has no default value and you have to assign a value.
          Otherwise, you can’t start connector.
        - connectors[i].definitions[j].internal (**string**) — true if this
          setting is assigned by system automatically.
        - connectors[i].definitions[j].tableKeys (**array(string)**) — the
          column name when the type is TABLE

Apart from official settings (topics, columns, etc), a connector also
have custom settings. Those setting definition can be found through
`GET <#get-a-worker-cluster>`__ or
`LIST <#list-all-workers-clusters>`__. And for another, the worker
cluster needs to take some time to load available connectors. If you
don’t see the setting definitions, please retry it later.


Setting Type
~~~~~~~~~~~~

The type of value includes two processes to input value when you are
trying to run a connector. For example, starting a connector will fail
when you input a string to a setting having **int** type. The acceptable
types are shown below.

#. Boolean — the value must be castable to **java.lang.Boolean**
#. String — the value must be castable to **java.lang.String**
#. Short — the value must be castable to **java.lang.Short**
#. Int — the value must be castable to **java.lang.Integer**
#. Long — the value must be castable to **java.lang.Long**
#. Double — the value must be castable to **java.lang.Double**
#. Class — the value must be castable to **java.lang.String** and it must be equal to a class in worker’s jvm
#. Password — the value must be castable to **java.lang.String**. the value is replaced by **hidden** in APIs
#. List — the value must be castable to **java.lang.String** and it is split according to JSON array
#. Table — the value must be castable to **java.lang.String** and it has the following JSON representation.
#. Duration — the value must be castable to **java.time.Duration** and it is based on the ISO-860 duration
   format PnDTnHnMn.nS

.. code-block:: json

   [
     {
       "order": 1,
       "c0": "v0",
       "c1": "v1",
       "c2": "v2"
     },
     {
       "order": 2,
       "c0": "t0",
       "c1": "t1",
       "c2": "t2"
     }
   ]

How to get the description of above **keys**? If the setting type is
**table**, the setting must have **tableKeys**. It is a array of string
which shows the keys used in the table type. For instance, a setting
having table type is shown below.

.. code-block:: json

   {
     "reference": "NONE",
     "displayName": "columns",
     "internal": false,
     "documentation": "output schema",
     "valueType": "TABLE",
     "tableKeys": [
       "order",
       "dataType",
       "name",
       "newName"
     ],
     "orderInGroup": 6,
     "key": "columns",
     "required": false,
     "defaultValue": null,
     "group": "core",
     "editable": true
   }


Setting Reference
~~~~~~~~~~~~~~~~~

This element is a specific purpose. It is used by ohara manager (UI)
only. If you don’t have interest in UI, you can just ignore this
element. However, we still list the available values here.

#. TOPIC
#. WORKER_CLUSTER


create a worker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*POST /v0/workers*

1. name (**string**) — cluster name
2. imageName (**string**) — docker image
3. clientPort (**int**) — worker client port.
4. jmxPort (**int**) — worker jmx port.
5. brokerClusterName (**string**) — broker cluster used to host topics
   for this worker cluster
6. jarKeys (**array(object)**) — the “primary key” object list of jar
   that will be loaded by worker cluster

    - jarKeys[i].group (**string**) — the group name of jar
    - jarKeys[i].name (**string**) — the name of jar

1. groupId (**string**) — the id of worker stored in broker cluster
2. configTopicName (**string**) — a internal topic used to store connector configuration
3. configTopicReplications (**int**) — number of replications for config topic
4. offsetTopicName (**string**) — a internal topic used to store connector offset
5. offsetTopicReplications (**int**) — number of replications for offset topic
6. offsetTopicPartitions (**int**) — number of partitions for offset topic
7. statusTopicName (**string**) — a internal topic used to store connector status
8. statusTopicReplications (**int**) — number of replications for status topic
9. statusTopicPartitions (**int**) — number of partitions for status topic
10. nodeNames (**array(string)**) — the nodes running the worker process

**Example Request**

  .. code-block:: json

     {
       "name": "wk00",
       "imageName": "oharastream/connect-worker:$|version|",
       "clientPort": 12345,
       "jmxPort": 12346,
       "brokerClusterName": "preCreatedBkCluster",
       "groupId": "abcdefg",
       "configTopicName": "configTopic",
       "configTopicReplications": 1,
       "offsetTopicName": "offsetTopic",
       "offsetTopicReplications": 1,
       "offsetTopicPartitions": 1,
       "statusTopicName": "statusTopic",
       "statusTopicReplications": 1,
       "statusTopicPartitions": 1,
       "jarKeys": [
         {
           "group": "abc",
           "name": "myjar"
         }
       ],
       "nodeNames": [
         "node00"
       ]
     }

**Example Response**

  .. code-block:: json

     {
       "statusTopicName": "statusTopic",
       "name": "wk00",
       "offsetTopicPartitions": 1,
       "brokerClusterName": "preCreatedBkCluster",
       "connectors": [],
       "sinks": [],
       "offsetTopicName": "offsetTopic",
       "imageName": "oharastream/connect-worker:$|version|",
       "groupId": "abcdefg",
       "jarInfos": [],
       "statusTopicReplications": 1,
       "configTopicPartitions": 1,
       "offsetTopicReplications": 1,
       "configTopicReplications": 1,
       "statusTopicPartitions": 1,
       "configTopicName": "configTopic",
       "jmxPort": 12346,
       "clientPort": 12345,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": []
     }

  As mentioned before, ohara provides default to most settings. You can
  just input name, nodeNames and jars to run a worker cluster.

**Example Request**

  .. code-block:: json

     {
       "name": "wk00",
       "jarKeys": [
           {
             "group": "abc",
             "name": "myjar"
           }
       ],
       "nodeNames": [
         "node00"
       ]
     }

  .. note::
     As you don’t input the broker cluster name, Ohara will try to pick up
     a broker cluster for you. If the number of broker cluster host by
     ohara is only one, ohara do deploy worker cluster on the broker
     cluster. Otherwise, ohara will say that it can’t match a broker
     cluster for you. All ports have default value so you can ignore them
     when creating worker cluster. However, the port conflict detect does
     not allow you to reuse port on different purpose (a dangerous behavior, right?).

**Example Response**

.. code-block:: json

   {
     "statusTopicName": "status-89eaef1e9d",
     "name": "wk00",
     "offsetTopicPartitions": 1,
     "brokerClusterName": "preCreatedBkCluster",
     "connectors": [],
     "offsetTopicName": "offset-956c528fa5",
     "imageName": "oharastream/connect-worker:$|version|",
     "groupId": "dcafb19d0e",
     "jarInfos": [],
     "statusTopicReplications": 1,
     "configTopicPartitions": 1,
     "offsetTopicReplications": 1,
     "configTopicReplications": 1,
     "statusTopicPartitions": 1,
     "configTopicName": "setting-67c528ca7d",
     "jmxPort": 8084,
     "clientPort": 8083,
     "nodeNames": [
       "node00"
     ],
     "deadNodes": []
   }


list all workers clusters
~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/workers*

**Example Response**

  .. code-block:: json

     [
       {
         "statusTopicName": "status-89eaef1e9d",
         "name": "wk00",
         "offsetTopicPartitions": 1,
         "brokerClusterName": "preCreatedBkCluster",
         "connectors": [],
         "offsetTopicName": "offset-956c528fa5",
         "imageName": "oharastream/connect-worker:$|version|",
         "groupId": "dcafb19d0e",
         "jarInfos": [],
         "statusTopicReplications": 1,
         "configTopicPartitions": 1,
         "offsetTopicReplications": 1,
         "configTopicReplications": 1,
         "statusTopicPartitions": 1,
         "configTopicName": "setting-67c528ca7d",
         "jmxPort": 8084,
         "clientPort": 8083,
         "nodeNames": [
           "node00"
         ],
         "deadNodes": []
       }
     ]


delete a worker cluster
~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/workers/$name*

**Query Parameters**

#. force (**boolean**) — true if you don’t want to wait the graceful shutdown
   (it can save your time but may damage your data). Other values invoke graceful delete.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent worker cluster, and the response is
     204 NoContent.


get a worker cluster
~~~~~~~~~~~~~~~~~~~~

*GET /v0/workers/$name*

**Example Response**

  .. code-block:: json

     {
       "statusTopicName": "status-d7f7a35aa4",
       "name": "wk00",
       "offsetTopicPartitions": 1,
       "brokerClusterName": "preCreatedBkCluster",
       "connectors": [
         {
           "className": "com.island.ohara.connector.perf.PerfSource",
           "definitions": [
             {
               "reference": "NONE",
               "displayName": "connector.class",
               "internal": false,
               "documentation": "the class name of connector",
               "valueType": "CLASS",
               "tableKeys": [],
               "orderInGroup": 0,
               "key": "connector.class",
               "required": true,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "tasks.max",
               "internal": false,
               "documentation": "the number of tasks invoked by connector",
               "valueType": "INT",
               "tableKeys": [],
               "orderInGroup": 3,
               "key": "tasks.max",
               "required": true,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "key.converter",
               "internal": true,
               "documentation": "key converter",
               "valueType": "CLASS",
               "tableKeys": [],
               "orderInGroup": 4,
               "key": "key.converter",
               "required": false,
               "defaultValue": "org.apache.kafka.connect.converters.ByteArrayConverter",
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "value.converter",
               "internal": true,
               "documentation": "value converter",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 5,
               "key": "value.converter",
               "required": false,
               "defaultValue": "org.apache.kafka.connect.converters.ByteArrayConverter",
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "kind",
               "internal": false,
               "documentation": "kind of connector",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 11,
               "key": "kind",
               "required": false,
               "defaultValue": "source",
               "group": "core",
               "editable": false
             },
             {
               "reference": "NONE",
               "displayName": "connector.name",
               "internal": false,
               "documentation": "the name of connector",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 1,
               "key": "connector.name",
               "required": false,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "columns",
               "internal": false,
               "documentation": "output schema",
               "valueType": "TABLE",
               "tableKeys": [
                 "order",
                 "dataType",
                 "name",
                 "newName"
               ],
               "orderInGroup": 6,
               "key": "columns",
               "required": false,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "WORKER_CLUSTER",
               "displayName": "workerClusterName",
               "internal": false,
               "documentation": "the cluster name of running this connector.If there is only one worker cluster, you can skip this setting since configurator will pick up a worker cluster for you",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 7,
               "key": "workerClusterName",
               "required": false,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "TOPIC",
               "displayName": "topics",
               "internal": false,
               "documentation": "the topics used by connector",
               "valueType": "LIST",
               "tableKeys": [],
               "orderInGroup": 2,
               "key": "topics",
               "required": true,
               "defaultValue": null,
               "group": "core",
               "editable": true
             },
             {
               "reference": "NONE",
               "displayName": "version",
               "internal": false,
               "documentation": "version of connector",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 8,
               "key": "version",
               "required": false,
               "defaultValue": "$|version|",
               "group": "core",
               "editable": false
             },
             {
               "reference": "NONE",
               "displayName": "revision",
               "internal": false,
               "documentation": "revision of connector",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 9,
               "key": "revision",
               "required": false,
               "defaultValue": "8faa89f18370c891422dae1993def55795f7ef2e",
               "group": "core",
               "editable": false
             },
             {
               "reference": "NONE",
               "displayName": "author",
               "internal": false,
               "documentation": "author of connector",
               "valueType": "STRING",
               "tableKeys": [],
               "orderInGroup": 10,
               "key": "author",
               "required": false,
               "defaultValue": "root",
               "group": "core",
               "editable": false
             }
           ]
         }
       ],
       "offsetTopicName": "offset-2c564b55cf",
       "imageName": "oharastream/connect-worker:$|version|",
       "groupId": "a5b623d114",
       "jarInfos": [],
       "statusTopicReplications": 1,
       "configTopicPartitions": 1,
       "offsetTopicReplications": 1,
       "configTopicReplications": 1,
       "statusTopicPartitions": 1,
       "configTopicName": "setting-68be0e46f7",
       "jmxPort": 8084,
       "clientPort": 8083,
       "nodeNames": [
         "node00"
       ],
       "deadNodes": []
     }


add a new node to a running worker cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/workers/$name/$nodeName*

If you want to extend a running worker cluster, you can add a node to
share the heavy loading of a running worker cluster. However, the
balance is not triggered at once. By the way, moving a task to another
idle node needs to **stop** task first. Don’t worry about the temporary
lower throughput when balancer is running.

**Example Response**

  .. code-block:: json

     {
       "statusTopicName": "status-89eaef1e9d",
       "name": "wk00",
       "offsetTopicPartitions": 1,
       "brokerClusterName": "preCreatedBkCluster",
       "connectors": [],
       "offsetTopicName": "offset-956c528fa5",
       "imageName": "oharastream/connect-worker:$|version|",
       "groupId": "dcafb19d0e",
       "jarInfos": [],
       "statusTopicReplications": 1,
       "configTopicPartitions": 1,
       "offsetTopicReplications": 1,
       "configTopicReplications": 1,
       "statusTopicPartitions": 1,
       "configTopicName": "setting-67c528ca7d",
       "jmxPort": 8084,
       "clientPort": 8083,
       "nodeNames": [
         "node01",
         "node00"
       ],
       "deadNodes": []
     }

remove a node from a running worker cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*DELETE /v0/workers/$name/$nodeName*

If your budget is limited, you can decrease the number of nodes running
worker cluster. BUT, removing a node from a running worker cluster
invoke a lot of task move, and it will decrease the throughput of your
connector.

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent worker node, and the response is
     204 NoContent.


Validation
----------

Notwithstanding we have read a lot of document and guideline, there is a
chance to input incorrect request or settings when operating ohara.
Hence, ohara provides a serial APIs used to validate request/settings
before you do use them to start service. Noted that not all
request/settings are validated by Ohara configurator. If the
request/settings is used by other system (for example, kafka), ohara
automatically bypass the validation request to target system and then
wrap the result to JSON representation.


Validate the FTP connection
~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/validate/ftp*

The parameters of request are shown below:

#. hostname (**string**) — ftp server hostname
#. port (**int**) — ftp server port 
#. user (**string**) — account of ftp server
#. password (**string**) — password of ftp server
#. workerClusterName (**string**) — the target cluster used to validate this connection

**Example Request**

  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "user",
       "password": "pwd"
     }

  .. note::

     Ohara picks up the single worker cluster directly when you ignore the
     element of worker cluster.

Since FTP connection is used by ftp connector only, ohara configurator
involves several connectors to test the connection properties. Ohara
configurator collects report from each connectors and then generate a
JSON response shown below.

#. hostname (**string**) — the node which execute this validation
#. message (**string**) — the description about this validation
#. pass (**boolean**) — true is pass

**Example Request**

  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to ftp server",
         "pass": true
       }
     ]


Validate the JDBC connection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/validate/rdb*

The parameters of request are shown below:

#. url (**string**) — jdbc url
#. user (**string**) — account of db server 
#. password (**string**) — password of db server
#. workerClusterName (**string**) — the target cluster used to validate this connection

**Example Response**

  .. code-block:: json

     {
       "url": "jdbc://",
       "user": "user",
       "password": "pwd",
       "tableNames": [
         "table0", "table1"
       ]
     }

  .. note::
     Ohara picks up the single worker cluster directly when you ignore the
     element of worker cluster.

Since JDBC connection is used by jdbc connector only, ohara configurator
involves several connectors to test the connection properties. Ohara
configurator collects report from each connectors and then generate a
JSON response shown below:

#. hostname (**string**) — the node which execute this validation
#. message (**string**) — the description about this validation
#. pass (**boolean**) — true is pass
#. tableNames (**array(String)**) — the table readable to passed user/password

**Example Response**

  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to db server",
         "pass": true
       }
     ]


Validate the HDFS connection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/validate/hdfs*

The parameters of request are shown below.

#. uri (**string**) — hdfs url
#. workerClusterName (**string**) — the target cluster used to validate this connection

**Example Request**

  .. code-block:: json

     {
       "uri": "file://"
     }

  .. note::
    Ohara picks up the single worker cluster directly when you ignore the
    element of worker cluster.

Since HDFS connection is used by hdfs connector only, ohara configurator
involves several connectors to test the connection properties. Ohara
configurator collects report from each connectors and then generate a
JSON response shown below:

#. hostname (**string**) — the node which execute this validation
#. message (**string**) — the description about this validation
#. pass (**boolean**) — true is pass

**Example Response**

  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to hdfs server",
         "pass": true
       }
     ]


Validate the node connection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/validate/node*

The parameters of request are shown below:

#. hostname (**string**) — hostname of node
#. port (**int**) — ssh port of node 
#. user (**string**) — ssh account
#. password (**string**) — ssh password

**Example Request**

  .. code-block:: json
  
     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

Since Node connection is used by ohara configurator only, ohara
configurator validates the connection by itself. The format of report is
same to other reports but the **hostname** is fill with **node’s
hostname** rather than node which execute the validation. 1. hostname
(**string**) — node’s hostname 1. message (**string**) — the description
about this validation 1. pass (**boolean**) — true is pass

**Example Response**

  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to ssh server",
         "pass": true
       }
     ]


Validate the connector settings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*PUT /v0/validate/connector*

Before starting a connector, you can send the settings to test whether
all settings are available for specific connector. Ohara is not in
charge of settings validation. Connector MUST define its setting via
`setting definitions <custom_connector.html#setting-definitions>`__.
Ohara configurator only repackage the request to kafka format and then
collect the validation result from kafka.

**Example Request**

  The request format is same as `connector request <#create-the-settings-of-connector>`__

**Example Response**

  If target connector has defined the settings correctly, kafka is doable
  to validate each setting of request. Ohara configurator collect the
  result and then generate the following report.

  .. code-block:: json

     {
       "errorCount": 0,
       "settings": [
         {
           "definition": {
             "reference": "NONE",
             "displayName": "connector.class",
             "internal": false,
             "documentation": "the class name of connector",
             "valueType": "CLASS",
             "tableKeys": [],
             "orderInGroup": 0,
             "key": "connector.class",
             "required": true,
             "defaultValue": null,
             "group": "core",
             "editable": true
           },
           "setting": {
             "key": "connector.class",
             "value": "com.island.ohara.connector.perf",
             "errors": []
           }
         }
       ]
     }

The above example only show a part of report. The element **definition**
is equal to `connector’s setting definition <#worker>`__. The definition
is what connector must define. If you don’t write any definitions for
you connector, the validation will do nothing for you. The element
**setting** is what you request to validate.

#. key (**string**) — the property key. It is equal to key in **definition**
#. value (**string**) — the value you request to validate
#. errors (**array(string)**) — error message when the input value is illegal to connector


Container
---------

Each processes managed by ohara is based on docker container. In most
cases, user don’t need to know the details of containers since the
management of containers is on ohara’s shoulder. However, ohara
understand that we all have curious brain so ohara supports to display
the container’s details of a running cluster. Noted that the context may
be changed between different release of ohara. And the distinct
implementations of container manager possibly provide different context
of containers.


retrieve the container details of a running cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/containers/$clusterName*

**Example Response**

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


StreamApp
---------

Ohara StreamApp is a unparalleled wrap of kafka streaming. It leverages
and enhances `kafka streams <https://kafka.apache.org/documentation/streams/>`__ to make
developer easily design and implement the streaming application. More
details of developing streaming application is in `custom stream guideline <custom_streamapp.html>`__.

Assume that you have completed a streaming application via ohara Java
APIs, and you have generated a jar including your streaming code. By
Ohara Restful APIs, you are enable to control, deploy, and monitor
your streaming application. As with cluster APIs, ohara leverages
docker container to host streaming application. Of course, you can
apply your favor container management tool including simple (based on ssh)
and k8s when you are starting ohara.

Before stating to use restful APIs, please ensure that all nodes have
downloaded the `StreamApp image <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/streamapp>`__.
The jar you uploaded to run streaming application will be included in
the image and then executes as a docker container. The `StreamApp image <https://cloud.docker.com/u/oharastream/repository/docker/oharastream/streamapp>`__
is kept in each node so don’t worry about the network. We all hate
re-download everything when running services.

The following information of StreamApp are updated by ohara.

#. name (**string**) — custom name of this streamApp
#. imageName (**string**) — image name of this streamApp
#. instances (**int**) — numbers of streamApp container
#. nodeNames (**array(string)**) — node list of streamApp running container
#. deadNodes (**array(string)**) — dead node list of the exited containers from this cluster
#. jar (**object**) — uploaded jar key
#. from (**array(string)**) — topics of streamApp consume with
#. to (**array(string)**) — topics of streamApp produce to
#. state (**option(string)**) — only started/failed streamApp has state
#. jmxPort (**int**) — the expose jmx port 
#. `metrics <custom_connector.html#metrics>`__ (**object**) — the metrics from this streamApp.
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create the properties of a streamApp.

*POST /v0/stream*

**Example Request**

1. name (**string**) — new streamApp name. This is the object unique name.
    - The acceptable char is [0-9a-z]
    - The maximum length is 20 chars

2. imageName (**option(string)**) — image name of streamApp used to ;
   default is official streamapp image of current version
3. jar (**object**) — the used jar object
    - jar.group (**string**) — the group name of this jar
    - jar.name (**string**) — the name without extension of this jar

4. from (**option(array(string))**) — new source topics ; default is empty
5. to (**option(array(string))**) — new target topics ; default is empty
6. jmxPort (**option(int)**) — expose port for jmx ; default is random port
7. instances (**option(int)**) — number of running streamApp ; default is 1
8. nodeNames (**option(array(string))**) — node name list of streamApp used to ; default is empty

.. code-block:: json

   {
     "name": "myapp",
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

**Example Response**

1.  name (**string**) — custom name of this streamApp
2.  imageName (**string**) — image name of this streamApp
3.  instances ( **int**) — numbers of streamApp container
4.  nodeNames (**array(string)**) — node list of streamApp running
    container
5.  deadNodes (**array(string)**) — dead node list of the exited
    containers from this cluster
6.  jar (**object**) — uploaded jar key
7.  from (**array(string)**) — topics of streamApp consume with
8.  to (**array(string)**) — topics of streamApp produce to
9.  state (**option(string)**) — only started/failed streamApp has state
10. jmxPort (**int**) — the expose jmx port
11. `metrics <custom_connector.html#metrics>`__ (**object**) — the
    metrics from this streamApp.

    - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of query metrics from remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

12. exactlyOnce (**boolean**) — enable exactly once
13. error (**option(string)**) — the error message from a failed
    streamApp. If the streamApp is fine or un-started, you won’t get
    this field.
14. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     {
       "name": "myapp",
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


get information from a specific streamApp cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/stream/${name}*

**Example Response**

1.  name (**string**) — custom name of this streamApp
2.  imageName (**string**) — image name of this streamApp
3.  instances ( **int**) — numbers of streamApp container
4.  nodeNames (**array(string)**) — node list of streamApp running
    container
5.  deadNodes (**array(string)**) — dead node list of the exited
    containers from this cluster
6.  jar (**object**) — uploaded jar key
7.  from (**array(string)**) — topics of streamApp consume with
8.  to (**array(string)**) — topics of streamApp produce to
9.  state (**option(string)**) — only started/failed streamApp has state
10. jmxPort (**int**) — the expose jmx port
11. `metrics <custom_connector.html#metrics>`__ (**object**) — the metrics from this streamApp.

    - meters (**array(object)**) — the metrics in meter type

      - meters[i].value (**double**) — the number stored in meter
      - meters[i].unit (**string**) — unit for value
      - meters[i].document (**string**) — document of this meter
      - meters[i].queryTime (**long**) — the time of record generated in remote machine
      - meters[i].startTime (**option(long)**) — the time of record generated in remote machine
12. exactlyOnce (**boolean**) — enable exactly once
13. error (**option(string)**) — the error message from a failed
    streamApp. If the streamApp is fine or un-started, you won’t get
    this field.
14. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     {
       "name": "myapp",
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

update properties of specific streamApp
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Update the properties of a non-started streamApp.

*PUT /v0/stream/${name}*

**Example Request**

1. imageName (**option(string)**) — new streamApp image name
2. from (**option(array(string))**) — new source topics
3. to (**option(array(string))**) — new target topics
4. jar (**option(object)**) — new uploaded jar key
5. jmxPort (**option(int)**) — new jmx port
6. instances (**option(int)**) — new number of running streamApp
7. nodeNames (**option(array(string))**) — new node name list of
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

**Example Response**

1.  name (**string**) — custom name of this streamApp
2.  imageName (**string**) — image name of this streamApp
3.  instances ( **int**) — numbers of streamApp container
4.  nodeNames (**array(string)**) — node list of streamApp running
    container
5.  deadNodes (**array(string)**) — dead node list of the exited
    containers from this cluster
6.  jar (**object**) — uploaded jar key
7.  from (**array(string)**) — topics of streamApp consume with
8.  to (**array(string)**) — topics of streamApp produce to
9.  state (**option(string)**) — only started/failed streamApp has state
10. jmxPort (**int**) — the expose jmx port
11. `metrics <custom_connector.html#metrics>`__ (**object**) — the
    metrics from this streamApp.

    - meters (**array(object)**) — the metrics in meter type

       - meters[i].value (**double**) — the number stored in meter
       - meters[i].unit (**string**) — unit for value
       - meters[i].document (**string**) — document of this meter
       - meters[i].queryTime (**long**) — the time of query metrics from remote machine
       - meters[i].startTime (**option(long)**) — the time of record generated in remote machine

12. exactlyOnce (**boolean**) — enable exactly once
13. error (**option(string)**) — the error message from a failed
    streamApp. If the streamApp is fine or un-started, you won’t get
    this field.
14. lastModified (**long**) — last modified this jar time

  .. code-block:: json

     {
       "name": "myapp",
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
~~~~~~~~~~~~~~~~~

*PUT /v0/stream/${name}/start*

**Example Response**

  .. code-block:: json

     {
       "name": "myapp",
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


stop a StreamApp
~~~~~~~~~~~~~~~~

This action will graceful stop and remove all docker containers belong
to this streamApp. Note: successful stop streamApp will have no status.

*PUT /v0/stream/${name}/stop*

**Example Response**

  .. code-block:: json

     {
       "name": "myapp",
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

[TODO] This is not implemented yet !

*GET /v0/stream/view/${name}*

**Example Response**

1. jarInfo (**string**) — the upload jar information
2. name (**string**) — the streamApp name
3. poneglyph (**object**) — the streamApp topology tree graph

-  steles (**array(object)**) — the topology collection

   -  steles[i].kind (**string**) — this component kind (SOURCE,
      PROCESSOR, or SINK)
   -  steles[i].key (**string**) — this component kind with order
   -  steles[i].name (**string**) — depend on kind, the name is

      -  SOURCE — source topic name
      -  PROCESSOR — the function name
      -  SINK — target topic name

   -  steles[i].from (**string**) — the prior component key (could be
      empty if this is the first component)
   -  steles[i].to (**string**) — the posterior component key (could be
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


Files
-----

Ohara encourages user to write custom application if the official
applications can satisfy requirements for your use case. Jar APIs is a
useful entry of putting your jar on ohara and then start related
services with it. For example, `Worker APIs <#create-a-worker-cluster>`__ 
accept a **jars** element which can
carry the jar name pointing to a existent jar in ohara. The worker
cluster will load all connectors of the input jar, and then you are able
to use the connectors on the worker cluster.

The properties stored by ohara are shown below.

#. name (**string**) — the file name without extension
#. group (**string**) — the group name (we use this field to separate different workspaces)
#. size (**long**) — file size
#. url (**option(string)**) — url to download this jar from Ohara Configurator. Noted not all jars are downloadable to user.
#. lastModified (**long**) — the time of uploading this file


upload a file to ohara
~~~~~~~~~~~~~~~~~~~~~~

Upload a file to ohara with field name : “jar” and group name : “group”
the text field “group” could be empty and we will generate a random
string.

*POST /v0/files*

**Example Request**

  .. code-block:: http

     Content-Type: multipart/form-data
     file="aa.jar"
     group="wk01"
     tags={}

  .. note::
     You have to specify the file name since it is a part of metadata
     stored by ohara. Noted, the later uploaded file can overwrite the
     older one

**Example Response**

  .. code-block:: json

     {
       "name": "aa.jar",
       "group": "wk01",
       "size": 1779,
       "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
       "lastModified": 1561012496975
     }


list all jars
~~~~~~~~~~~~~

Get all jars from specific group of query parameter. If no query
parameter, wll return all jars.

*GET /v0/files?group=wk01*

**Example Response**

  .. code-block:: json

     [
       {
         "name": "aa.jar",
         "group": "wk01",
         "size": 1779,
         "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
         "lastModified": 1561012496975
       }
     ]


delete a file
~~~~~~~~~~~~~

Delete a file with specific name and group. Note: the query parameter
must exists.

*DELETE /v0/files/$name?group=wk01*

**Example Response**

  ::

     204 NoContent

  .. note::
     It is ok to delete an nonexistent jar, and the response is 204
     NoContent. If you delete a file is used by other services, you also
     break the scalability of service as you can’t run the jar on any new
     nodes


get a file
~~~~~~~~~~

Get a file with specific name and group. Note: the query parameter must
exists.

*GET /v0/files/$name?group=wk01*

**Example Response**

  .. code-block:: json

     {
         "name": "aa.jar",
         "group": "wk01",
         "size": 1779,
         "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
         "lastModified": 1561012496975
     }


update tags of file
~~~~~~~~~~~~~~~~~~~

*PUT /v0/files/$name?group=wk01*

**Example Response**

  .. code-block:: json

     {
       "tags": {
         "a": "b"
       }
     }

  .. note::
     it returns error code if input group/name are not associated to an
     existent file.

**Example Response**

  .. code-block:: json

     {
         "name": "aa.jar",
         "group": "wk01",
         "size": 1779,
         "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
         "lastModified": 1561012496975,
         "tags": {
             "a": "b"
           }
     }


Logs
----

This world is beautiful but not safe. Even though ohara shoulders the
blame for simplifying your life, there is a slim chance that something
don’t work well in ohara. The Logs APIs, which are engineers’ best
friend, open a door to observe the logs of running cluster.

It collect output from all containers’ of a cluster and then format them
to JSON representation which has following elements.

#. name (**string**) — cluster name
#. logs (**array(object)**) — log of each container
    - logs[i].name — container’s name
    - logs[i].value — total output of a container


get the log of a running cluster
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*GET /v0/logs/$clusterType/$clusterName*

- clusterType (**string**)
    - zookeepers
    - brokers
    - workers

**Example Response**

  .. code-block:: json

     {
       "name": "precreatezkcluster",
       "logs": [
         {
           "name": "node00",
           "value": "2019-04-15 02:13:33,168 [myid:] - INFO [main:QuorumPeerConfig@136"
         }
       ]
     }


Query
-----

Query APIs is a collection of helper methods required by Ohara Manager
so you should assume this APIs are **private** and we do not guarantee
compatibility to this APIs. Normally, Ohara Configurator can’t run the
query for you since most queries demand specific dependencies in
runtime, and we don’t allow you to touch the classpath of Ohara
Configurator. Hence, Ohara Configurator pass the queries to official
specific **connectors** to execute the queries on a `worker cluster <#worker>`__.
It implies that you should set up a `worker cluster <#worker>`__ before submitting query request to Ohara
Configurator.


Query Database
~~~~~~~~~~~~~~

*POST /v0/query/rdb*

This API returns the table details of a relational database. This API
invokes a running connector on worker cluster to fetch database
information and return to Ohara Configurator. You should deploy suitable
jdbc driver on worker cluster before using this API. Otherwise, you will
get a exception returned by Ohara Configurator. The query consists of
following fields.

1. url (**string**) — jdbc url
2. user (**string**) — user who can access target database
3. password (**string**) — password which can access target database
4. workerClusterName (**string**) — used to execute connectors to fetch table information
5. catalogPattern (**option(string)**) — filter returned tables according to catalog
6. schemaPattern (**option(string)**) — filter returned tables according to schema
7. tableName (**option(string)**) — filter returned tables according to name

**Example Request**

  .. code-block:: json

     {
       "url": "jdbc:sqlserver://",
       "user": "abc",
       "password": "abc",
       "workerClusterName": "wk00"
     }

**Example Response**

  1. name (**string**) — database name
  2. tables (**array(object)**)

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


Info
----

Info API returns the information of Ohara Configurator you are
executing. It consists of following fields.

#. versionInfo (**object**) — version details of Ohara Configurator
    - version (**string**) — the release version of Ohara Configurator
    - revision (**string**) — commit hash of Ohara Configurator. You can trace the hash code via `Github <https://github.com/oharastream/ohara/commits/master>`__
    - user (**string**) — the release manager of Ohara Configurator.
    - date (**string**) — the date of releasing Ohara Configurator.
#. mode (**string**) — the mode of this configurator. There are three modes now:
    - First, k8s mode is for the production.
    - Second, ssh is useful to simple env.
    - Third, fake mode is used to test APIs.

**Example Response**

  .. code-block:: json

     {
      "versionInfo": {
        "version": "$|version|",
        "user": "chia",
        "revision": "b86742ca03a0ca02cc3578f8686e38e5cf2fb461",
        "date": "2019-05-13 09:59:38"
      },
      "mode": "ssh"
     }
