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

.. _rest-connectors:

Connector
=========

Connector is core of application in ohara :ref:`pipeline <rest-pipelines>`.
Connector has two type - source and sink. Source connector pulls data
from another system and then push to topic. By contrast, Sink connector
pulls data from topic and then push to another system. In order to use
connector in :ref:`pipeline <rest-pipelines>`, you have to set up a connector
settings in ohara and then add it to :ref:`pipeline <rest-pipelines>`. Of
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
#. numberOfTasks (**int**) — the number of tasks
#. workerClusterName (**string**) — target worker cluster
#. tags (**object**) — the extra description to this object

The following information are updated by Ohara.

#. group (**string**) — connector’s group
#. name (**string**) — connector’s name
#. lastModified (**long**) — the last time to update this connector
#. state (**option(string)**) — the state of a started connector.
   If the connector is not started, you won’t see this field
#. error (**option(string)**) — the error message from a failed connector.
   If the connector is fine or un-started, you won’t get this field.
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


.. _rest-connectors-create-settings:

create the settings of connector
--------------------------------

*POST /v0/connectors*

It is ok to lack some common settings when creating settings for a
connector. However, it is illegal to start a connector with incomplete
settings. For example, storing the settings consisting of only
**connector.name** is ok. But stating a connector with above incomplete
settings will introduce a error.

Example Request
  .. code-block:: json

     {
       "name": "jdbc_name",
       "connector.class": "com.island.ohara.connector.ftp.FtpSource"
     }

Example Response
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
--------------------------------

*PUT /v0/connectors/${name}?group=${group}*

  .. note::
    you cannot delete a non-stopped connector.

Example Request

  .. code-block:: json

     {
       "name": "jdbc_name",
       "connector.class": "com.island.ohara.connector.ftp.FtpSource"
     }

Example Response
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
----------------------------------

*GET /v0/connectors*

Example Response
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
------------------

*DELETE /v0/connectors/${name}?group=${group}*

Deleting the settings used by a running connector is not allowed. You
should :ref:`stop <rest-connectors-stop>` connector before deleting it.

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent connector or a running
     connector, and the response is 204 NoContent.


.. _rest-connectors-get-info:

get information of connector
----------------------------

*GET /v0/connectors/${name}?group=${group}*

Example Response
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
-----------------

*PUT /v0/connectors/${name}/start?group=${group}*

Ohara will send a start request to specific worker cluster to start the
connector with stored settings, and then make a response to called. The
connector is executed async so the connector may be still in starting
after you retrieve the response. You can send
:ref:`GET request <rest-connectors-get-info>` to see the state of
connector. This request is idempotent so it is safe to retry this
command repeatedly.

Example Response
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


.. _rest-connectors-stop:

stop a connector
----------------

*PUT /v0/connectors/${name}/stop?group=${group}*

Ohara will send a stop request to specific worker cluster to stop the
connector. The stopped connector will be removed from worker cluster.
The settings of connector is still kept by ohara so you can start the
connector with same settings again in the future. If you want to delete
the connector totally, you should stop the connector and then
:ref:`delete <rest-connectors-delete>` it. This request is idempotent so it is
safe to send this request repeatedly.

Example Response
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
-----------------

*PUT /v0/connectors/${name}/pause?group=${group}*

Pausing a connector is to disable connector to pull/push data from/to
source/sink. The connector is still alive in kafka. This request is
idempotent so it is safe to send this request repeatedly.

Example Response
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
------------------

*PUT /v0/connectors/${name}/resume?group=${group}*

Resuming a connector is to enable connector to pull/push data from/to
source/sink. This request is idempotent so it is safe to retry this
command repeatedly.

Example Response
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

