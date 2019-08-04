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


HDFS Connection Information
===========================

Ohara supports to store the simple hdfs information which is running on
single namenode without security configuration.

#. name (**string**) — name of this hdfs information.
#. uri (**string**) — hdfs connection information. The form looks like
   "hdfs://namenode:9999/"
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

The following information are tagged by ohara.

#. lastModified (**long**) — the last time to update this hdfs
   information


store a hdfs information
------------------------

*POST /v0/hdfs*

#. name (**string**) — name of this hdfs information.
#. uri (**string**) — hdfs connection information. The form looks like
   "hdfs://namenode:9999/"
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

Example Request
  .. code-block:: json

     {
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999"
     }

  .. note::
     the default value of group is “default”

Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }


update a hdfs information
-------------------------

*PUT /v0/hdfs/$name?group=$group*

#. name (**string**) — name of this hdfs information.
#. uri (**option(string)**) — hdfs connection information. The form
   looks like "hdfs://namenode:9999/"
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

Example Request
  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999"
     }

  .. note::
     This API creates an new object if input name does not exist.
     the default value of group is "default"

Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }


list all hdfs information stored in Ohara
-----------------------------------------

*GET /v0/hdfs*

Example Response
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
-------------------------

*DELETE /v0/hdfs/$name?group=$group*

#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

  .. note::
     the default value of group is “default”

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent hdfs information, and
     the response is 204 NoContent.


get a hdfs information
----------------------

*GET /v0/hdfs/$name?group=$group*

#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

  .. note::
     the default value of group is "default"

Example Response
  .. code-block:: json

     {
       "group": "default",
       "name": "hdfs0",
       "uri": "hdfs://namenode:9999",
       "lastModified": 1553498552595,
       "tags": {}
     }


