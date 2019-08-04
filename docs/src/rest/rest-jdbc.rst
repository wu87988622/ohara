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


JDBC Connection Information
===========================

Database is a common data source in our world. Ohara also supports to
link database to be a part of streaming, so there are also APIs which
help us to store related information used to connect database. Given
that we are in java world, the jdbc is only supported now. The storable
information is shown below.

#. name (**string**) — name of this jdbc information.
#. url (**string**) — jdbc connection information. format:

   jdbc:$database://$serverName\$instanceName:$portNumber
#. user (**string**) — the account which has permission to access
   database
#. password (**string**) — password of account. It is stored as text in
   Ohara
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

The following information are tagged by Ohara.

#. lastModified (**long**) — the last time to update this jdbc
   information


store a jdbc information
------------------------

*POST /v0/jdbc*

#. name (**string**) — name of this jdbc information.
#. url (**string**) — jdbc connection information. format:
   jdbc:$database://$serverName\$instanceName:$portNumber
#. user (**string**) — the account which has permission to access
   database
#. password (**string**) — password of account. It is stored as text in
   ohara
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

Example Request
  .. code-block:: json

     {
       "name": "jdbc_name",
       "url": "jdbc:mysql",
       "user": "user",
       "password": "aaa"
     }

  .. note::
     the default value of group is “default”

Example Response
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
-------------------------

*PUT /v0/jdbc/$name?group=$group*

#. name (**string**) — name of this jdbc information.
#. url (**option(string)**) — jdbc connection information. format:
   jdbc:$database://$serverName\$instanceName:$portNumber
#. user (**option(string)**) — the account which has permission to
   access database
#. password (**option(string)**) — password of account. It is stored as
   text in ohara
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

Example Request
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

Example Response
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


list all jdbc information stored in Ohara
-----------------------------------------

*GET /v0/jdbc*

Example Response
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
-------------------------

*DELETE /v0/jdbc/$name?group=$group*

#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is “default”

  .. note::
     the default value of group is “default”

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent jdbc information, and
     the response is 204 NoContent.


get a jdbc information
----------------------

*GET /v0/jdbc/$name?group=$group*

  .. note::
     the default value of group is “default”

Example Response
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

