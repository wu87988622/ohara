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


Info
====

Info API returns the information of service which is able be controlled by Ohara Configurator. It includes

#. Ohara Configurator
#. zookeeper
#. broker
#. worker

get Ohara Configurator info
---------------------------

*GET /v0/info/configurator*

the format of response of Ohara Configurator is shown below.

#. versionInfo (**object**) — version details of Ohara Configurator

   - branch (**string**) — the branch name of Ohara Configurator
   - version (**string**) — the release version of Ohara Configurator
   - revision (**string**) — commit hash of Ohara Configurator. You can trace the hash code via `Github <https://github.com/oharastream/ohara/commits/master>`__
   - user (**string**) — the release manager of Ohara Configurator.
   - date (**string**) — the date of releasing Ohara Configurator.

#. mode (**string**) — the mode of this configurator. There are three modes now:

   - K8S: k8s mode is for the production.
   - SSH: ssh is useful to simple env.
   - FAKE: fake mode is used to test APIs.

Example Response
  .. code-block:: json

    {
      "versionInfo": {
        "branch": "$|branch|",
        "version": "$|version|",
        "user": "chia",
        "revision": "b86742ca03a0ca02cc3578f8686e38e5cf2fb461",
        "date": "2019-05-13 09:59:38"
      },
      "mode": "FAKE"
    }

get zookeeper/broker/worker info
--------------------------------

*GET /v0/info/$service*

the available variables for $service are shown below.

#. zookeeper
#. broker
#. worker

Example Request

GET /v0/info/zookeeper

Example Response
  .. code-block:: json

    {
      "imageName": "oharastream/zookeeper:$|version|",
      "settingDefinitions": [
        {
          "reference": "NONE",
          "displayName": "group",
          "internal": false,
          "documentation": "group of this worker cluster",
          "valueType": "STRING",
          "tableKeys": [],
          "orderInGroup": 1,
          "key": "group",
          "required": false,
          "defaultValue": "default",
          "group": "core",
          "editable": true
        }
      ]
    }
