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

Version
=======

We all love to see the version of software, right? Ohara provide a API
to display the details of version. It includes following information.

#. version (**string**) — version of configurator
#. branch (**string**) from which ohara repo branch
#. user (**string**) — builder of configurator
#. revision (**string**) — latest commit of configurator
#. date (**string**) — build date of configurator


get the version of Ohara
------------------------

*GET /v0/info*

Example Response
  .. code-block:: json

     {
       "versionInfo": {
         "version": "$|version|",
         "branch": "master",
         "user": "Chia-Ping Tsai",
         "revision": "9af9578041f069a9a452c7fda5f7ed7217c0deea",
         "date": "2019-03-21 17:55:06"
       }
     }

