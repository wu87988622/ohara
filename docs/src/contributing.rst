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

Contributing
============

All we love is only pull request so we have some rules used to make your
PR looks good for reviewers.

.. note:: Note that you should file a new issue to discuss the PR detail with us before submitting a PR.


Quick start
-----------

- Fork and clone the `oharastream/ohara`_ repo
- Install dependencies. See our `how_to_build`_ for development machine setup
- Create a branch with your PR with ``git checkout -b ${your-branch-name}``
- Push your PR to remote: ``git push origin ${your-branch-name}``
- Create the PR with GitHub web UI and wait for reviews from our committers


Testing commands in the pull request
------------------------------------

These commands will come in handy when you want to test your PR on our QA(CI server).
To start a QA run, you can simply leave a comment with one of the following commands in the PR:

.. note:: Note that the comment should contain the exact command as listed below, comments like **Please retry my PR**
  or **Bot, retry -fae** won't work:

- ``retry``: trigger a full QA run
- ``retry -fae``: trigger a full QA run even if there's fail test during the run
- ``retry ${moduleName}``: trigger a QA run for a specific module. If a module is named **ohara-awesome**, you can
  enter ``retry awesome`` to run the QA against this specific module. Note that the module prefix **ohara-** is not needed. Following are some examples:

  - ``retry manager``: run **ohara-manager**'s unit test.
  - ``retry configurator``: run **ohara-configurator**'s unit test.

  Ohara manager has a couple of different tests and can be run separately by using the above-mentioned ``retry`` command.

  - ``retry manager-api``: run manager's API tests
  - ``retry manager-ut``: run manager's unit tests
  - ``retry manager-e2e``: run manager's end-to-end tests

- ``run``: start both Configurator and Manager on jenkins server. If the specified PR makes some changes to UI,
  you can run this command to see the changes

The QA build status can be seen at the bottom of your PR.


Important things about pull request
-----------------------------------

**A pull request must...**

- Pass all tests
- Your PR should not make ohara unstable, if it does. It should be reverted ASAP.
- You can either run these tests on your local (see our `how_to_build <how_to_build.html>`__ for more info on how to run tests) or by opening the PR on our repo. These tests will be running on your CI server.
- Pass code style check. You can automatically fix these issues with a single command: ::

   gradle spotlessApply

- Address all reviewers' comments

**A pull request should...**

- Be as small in scope as possible. Large PR is often hard to review.
- Add new tests

**A pull request should not...**

-  Bring in new libraries (or updating libraries to new version) without prior discussion. Do not update the dependencies unless you have a good reason.
-  Bring in new module without prior discussion
-  Bring in new APIs for Configurator without prior discussion

.. _oharastream/ohara: https://github.com/oharastream/ohara
.. _how_to_build: how_to_build.html#gradle-commands