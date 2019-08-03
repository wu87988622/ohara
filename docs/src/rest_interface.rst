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
:ref:`zookeeper <rest-zookeepers>`, :ref:`broker <rest-brokers>` and
:ref:`worker <rest-workers>` to see more details.

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

