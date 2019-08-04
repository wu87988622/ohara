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

Info API returns the information of Ohara Configurator you are
executing. It consists of following fields:

#. versionInfo (**object**) — version details of Ohara Configurator

   - version (**string**) — the release version of Ohara Configurator
   - revision (**string**) — commit hash of Ohara Configurator. You can trace the hash code via `Github <https://github.com/oharastream/ohara/commits/master>`__
   - user (**string**) — the release manager of Ohara Configurator.
   - date (**string**) — the date of releasing Ohara Configurator.

#. mode (**string**) — the mode of this configurator. There are three modes now:

   - First, k8s mode is for the production.
   - Second, ssh is useful to simple env.
   - Third, fake mode is used to test APIs.

Example Response
  .. code-block:: json

     {
      "versionInfo": {
        "version": "$|version|",
        "user": "chia",
        "revision": "b86742ca03a0ca02cc3578f8686e38e5cf2fb461",
        "date": "2019-05-13 09:59:38"
      },
      "mode": "ssh"
     }
