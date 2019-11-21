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

The available query parameters are shown below.

#. sinceSeconds (**long**) — show the logs since relative time

It collect output from all containers’ of a cluster and then format them
to JSON representation which has following elements.

#. clusterKey (**object**) — cluster key

  - clusterKey.group (**string**) — cluster group
  - clusterKey.name (**string**) — cluster name

#. logs (**array(object)**) — log of each container

   - logs[i].hostname — hostname
   - logs[i].value — total log of a container


get the log of a running cluster
--------------------------------

*GET /v0/logs/$clusterType/$clusterName?group=$group*

- clusterType (**string**)

  - zookeepers
  - brokers
  - workers
  - streams

Example Response
  .. code-block:: json

    {
      "clusterKey": {
        "group": "default",
        "name": "precreatezkcluster"
      },
      "logs": [
        {
          "hostname": "node00",
          "value": "2019-04-15 02:13:33,168 [myid:] - INFO [main:QuorumPeerConfig@136"
        }
      ]
    }


get the log of Configurator
---------------------------

*GET /v0/logs/configurator*

Example Response
  .. code-block:: json

    {
      "clusterKey": {
        "group": "N/A",
        "name": "abc"
      },
      "logs": [
        {
          "hostname": "node00",
          "value": "2019-04-15 02:13:33,168 [myid:] - INFO [main:QuorumPeerConfig@136"
        }
      ]
    }

.. note::
  the Configurator MUST run on docker container and the node hosting Configurator MUST be added to Configurator via
  :ref:`Node APIs <rest-nodes>`