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

FTP Connection Information
==========================

You can store the ftp information in ohara if the data is used
frequently. Currently, all data are stored by text. The storable
information is shown below.

#. name (**string**) — name of this ftp information
#. hostname (**string**) — ftp server hostname
#. port (**int**) — ftp server port
#. user (**string**) — account of ftp server
#. password (**string**) — password of ftp server
#. tags (**object**) — the extra description to this object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

The following information are tagged by ohara.

#. lastModified (**long**) — the last time to update this ftp
   information


store a ftp information
-----------------------

*POST /v0/ftp*

1. name (**string**) — name of this ftp information
2. hostname (**string**) — ftp server hostname
3. port (**int**) — ftp server port
4. user (**string**) — account of ftp server
5. password (**string**) — password of ftp server
6. tags (**object**) — the extra description to this object
7. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

.. note::
   the string value can’t be empty or null. the port should be small
   than 65535 and larger than zero. the default value of group is
   "default"

Example Request
  .. code-block:: json

     {
       "name": "ftp0",
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd",
       "tags": ["a"]
     }

Example Response
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
------------------------

*PUT /v0/ftp/$name?group=$group*

#. name (**string**) — name of this ftp information
#. hostname (**option(string)**) — ftp server hostname
#. port (**option(int)**) — ftp server port
#. user (**option(string)**) — account of ftp server
#. password (**option(string)**) — password of ftp server
#. tags (**option(object)**) — the extra description to this
   object
#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

.. note::
   the string value can’t be empty or null. the port should be small
   than 65535 and larger than zero.

Example Request
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
     "default"

Example Response
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
----------------------------------------

*GET /v0/ftp*

Example Response
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
------------------------

*DELETE /v0/ftp/$name?group=$group*

#. group (**string**) — group of this ftp information. It is a optional
   argument, and the default value of group is "default"

  .. note::
     the default value of group is "default"

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an jar from an nonexistent ftp information, and
     the response is 204 NoContent.


get a ftp information
---------------------

*GET /v0/ftp/$name?group=$group*

  .. note::
     the default value of group is "default"

Example Response

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

