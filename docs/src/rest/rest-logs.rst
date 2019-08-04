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

.. _rest-logs:

Logs
====

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
--------------------------------

*GET /v0/logs/$clusterType/$clusterName*

- clusterType (**string**)

  - zookeepers
  - brokers
  - workers

Example Response
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

