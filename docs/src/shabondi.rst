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

.. _shabondi:

Shabondi
========

Shabondi service play the role of a http proxy service in the Pipeline of Ohara.
If you want to integrate Ohara pipeline with your application, Shabondi is a good choice.
Just send the simple http request to **Shabondi source** service, you can hand over your data to Pipeline for processing.
On the other hand, you can send the http request to **Shabondi sink** service to fetch the output data of the Pipeline.

Following is a simple diagram of Pipeline to demonstrate about both the source and sink of Shabondi:
  .. figure:: images/shabondi-pipeline.png
     :alt: Shabondi in the pipeline

Data format
------------

Both Shabondi source and sink use :ref:`Row <connector-datamodel>` in JSON format for data input and output.
Row is a table structure data defined in Ohara code base. A row is comprised of multiple cells. Each cell has its **name** and **value**.
Every row of your input data will be stored in the **Topic**.


A table:
  +---------+-------+-----+-------------------+---------+
  |         | name  | age | email             | career  |
  +---------+-------+-----+-------------------+---------+
  | (row 1) | jason | 42  | jason@example.com | Surgeon |
  +---------+-------+-----+-------------------+---------+
  | (row n) | ...   | ... | ...               | ...     |
  +---------+-------+-----+-------------------+---------+


Example of row using Java:
  .. code-block:: java

    import oharastream.ohara.common.data.Row;
    import oharastream.ohara.common.data.Cell;
    class ExampleOfRow {
        public static void main(String[] args) {
            Row row = Row.of(
                    Cell.of("name", "jason"),
                    Cell.of("age", "42"),
                    Cell.of("email", "jason@example.com"),
                    Cell.of("career", "Surgeon")
                    );
        }
    }

Example of row in JSON format
  .. code-block:: json

    {
      "name": "jason",
      "age": 42,
      "email": "jason@example.com",
      "career": "Surgeon"
    }




Deployment
-----------

There are two ways to deploy Shabondi service

* Ohara Manager: (TBD)
* Use Configurator :ref:`REST API <rest-shabondi>` to create and start Shabondi service.

After a Shabondi service is properly configured, deploy and successfully started. It's ready to receive or send requests via HTTP.

Source service API
---------------------

Shabondi source service receives single row message through HTTP requests and then writes to the connected **Topic**.

Send Row
^^^^^^^^

Send a JSON data of :ref:`Row <connector-datamodel>` to Shabondi source service.

Request
  POST /

Request body
  The row data in JSON format


Example 1 (Succeed)
  Request

  .. code-block:: http

    POST http://node00:58456 HTTP/1.1
    Content-Type: application/json

    {
      "name": "jason",
      "age": 42,
      "email": "jason@example.com",
      "career": "Surgeon"
    }

  Response

  .. code-block:: http

    HTTP/1.1 200 OK
    Server: akka-http/10.1.11
    Date: Tue, 19 May 2020 02:41:20 GMT
    Content-Type: text/plain; charset=UTF-8
    Content-Length: 2

    OK

Example 2 (Failure)

  Request

  .. code-block:: http

    GET http://node00:58456 HTTP/1.1


  Response

  .. code-block:: http

    HTTP/1.1 405 Method Not Allowed
    Server: akka-http/10.1.11
    Date: Tue, 19 May 2020 02:45:56 GMT
    Content-Type: text/plain; charset=UTF-8
    Content-Length: 90

    Unsupported method, please reference: https://ohara.readthedocs.io/en/0.10.x/shabondi.html


------------

Sink service API
-----------------

The Shabondi Sink service accepts the http request, and then reads the rows from the connected **Topic** and response it in JSON format.

Fetch Rows
^^^^^^^^^^

Request
  GET /groups/$groupName

Response
  The array of row in JSON format

Example 1 (Succeed)
  Request

  .. code-block:: http

    GET http://node00:58458/groups/g1 HTTP/1.1

  Response

  .. code-block:: http

    HTTP/1.1 200 OK
    Server: akka-http/10.1.11
    Date: Wed, 20 May 2020 06:18:44 GMT
    Content-Type: application/json
    Content-Length: 115

    [
      {
        "name": "jason",
        "age": 42,
        "email": "jason@example.com",
        "career": "Surgeon"
      },
      {
        "name": "robert",
        "age": 36,
        "email": "robert99@gmail.com",
        "career": "Teacher"
      }
    ]


Example - Failure response(Illegal group name)
  Request

  .. code-block:: http

    GET http://node00:58458/groups/g1-h HTTP/1.1

  Response

  .. code-block:: http

    HTTP/1.1 406 Not Acceptable
    Server: akka-http/10.1.11
    Date: Wed, 20 May 2020 07:34:10 GMT
    Content-Type: text/plain; charset=UTF-8
    Content-Length: 50

    Illegal group name, only accept alpha and numeric.
