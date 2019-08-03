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


Query
=====

Query APIs is a collection of helper methods required by Ohara Manager
so you should assume this APIs are **private** and we do not guarantee
compatibility to this APIs. Normally, Ohara Configurator can’t run the
query for you since most queries demand specific dependencies in
runtime, and we don’t allow you to touch the classpath of Ohara
Configurator. Hence, Ohara Configurator pass the queries to official
specific **connectors** to execute the queries on a :ref:`worker cluster <rest-workers>`.
It implies that you should set up a :ref:`worker cluster <rest-workers>` before submitting query request to Ohara
Configurator.


Query Database
--------------

*POST /v0/query/rdb*

This API returns the table details of a relational database. This API
invokes a running connector on worker cluster to fetch database
information and return to Ohara Configurator. You should deploy suitable
jdbc driver on worker cluster before using this API. Otherwise, you will
get a exception returned by Ohara Configurator. The query consists of
following fields.

#. url (**string**) — jdbc url
#. user (**string**) — user who can access target database
#. password (**string**) — password which can access target database
#. workerClusterName (**string**) — used to execute connectors to fetch table information
#. catalogPattern (**option(string)**) — filter returned tables according to catalog
#. schemaPattern (**option(string)**) — filter returned tables according to schema
#. tableName (**option(string)**) — filter returned tables according to name

Example Request
  .. code-block:: json

     {
       "url": "jdbc:sqlserver://",
       "user": "abc",
       "password": "abc",
       "workerClusterName": "wk00"
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

