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

.. _rest-validation:

Validation
==========

Notwithstanding we have read a lot of document and guideline, there is a
chance to input incorrect request or settings when operating ohara.
Hence, ohara provides a serial APIs used to validate request/settings
before you do use them to start service. Noted that not all
request/settings are validated by Ohara configurator. If the
request/settings is used by other system (for example, kafka), ohara
automatically bypass the validation request to target system and then
wrap the result to JSON representation.


Validate the FTP connection
---------------------------

*PUT /v0/validate/ftp*

The parameters of request are shown below:

#. hostname (**string**) — ftp server hostname
#. port (**int**) — ftp server port
#. user (**string**) — account of ftp server
#. password (**string**) — password of ftp server
#. workerClusterName (**string**) — the target cluster used to validate this connection

Example Request
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

Example Request
  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to ftp server",
         "pass": true
       }
     ]


Validate the JDBC connection
----------------------------

*PUT /v0/validate/rdb*

The parameters of request are shown below:

#. url (**string**) — jdbc url
#. user (**string**) — account of db server
#. password (**string**) — password of db server
#. workerClusterName (**string**) — the target cluster used to validate this connection

Example Response
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

Example Response
  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to db server",
         "pass": true
       }
     ]


Validate the HDFS connection
----------------------------

*PUT /v0/validate/hdfs*

The parameters of request are shown below.

#. uri (**string**) — hdfs url
#. workerClusterName (**string**) — the target cluster used to validate this connection

Example Request
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

Example Response
  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to hdfs server",
         "pass": true
       }
     ]


Validate the node connection
----------------------------

*PUT /v0/validate/node*

The parameters of request are shown below:

#. hostname (**string**) — hostname of node
#. port (**int**) — ssh port of node
#. user (**string**) — ssh account
#. password (**string**) — ssh password

Example Request
  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

Since Node connection is used by ohara configurator only, ohara
configurator validates the connection by itself. The format of report is
same to other reports but the **hostname** is fill with **node’s hostname**
rather than node which execute the validation.

#. hostname (**string**) — node’s hostname
#. message (**string**) — the description about this validation
#. pass (**boolean**) — true is pass

Example Response
  .. code-block:: json

     [
       {
         "hostname": "node00",
         "message": "succeed to connector to ssh server",
         "pass": true
       }
     ]


Validate the connector settings
-------------------------------

*PUT /v0/validate/connector*

Before starting a connector, you can send the settings to test whether
all settings are available for specific connector. Ohara is not in
charge of settings validation. Connector MUST define its setting via
:ref:`setting definitions <connector-setting-def>`.
Ohara configurator only repackage the request to kafka format and then
collect the validation result from kafka.

Example Request
  The request format is same as :ref:`connector request <rest-connector-create-settings>`

Example Response
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
is equal to :ref:`connector’s setting definition <rest-worker>`. The definition
is what connector must define. If you don’t write any definitions for
you connector, the validation will do nothing for you. The element
**setting** is what you request to validate.

#. key (**string**) — the property key. It is equal to key in **definition**
#. value (**string**) — the value you request to validate
#. errors (**array(string)**) — error message when the input value is illegal to connector


