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

.. _managerdev:

Ohara Manager Development Guideline
===================================

This module contains Ohara manager (an HTTP server powered by
`Node.js <https://nodejs.org/en/>`__) and Ohara manager client (A
web-based user interface built with `React.js <https://reactjs.org/>`__
). In the following docs, we refer to **Server** as Ohara manager and
**Client** as Ohara manager client.

.. _managerdev-initial-setup:

Initial machine setup
---------------------

1. Install `Node.js <https://nodejs.org/en/>`__ 10.16.3 or node.js >=10.16.3 and < 11.0.0

2. Install `Yarn <https://yarnpkg.com/en/docs/install#mac-stable>`__
   1.13.0 or greater

3. Make sure you’re in the ohara-manager root and use this command to
   setup the app: ``yarn setup``. This will install all the dependencies
   for both the **Server** and **Client** as well as creating a
   production build for the client.

4. **Optional**: If you’re using Visual Studio Code as your editor, have
   a look at our :ref:`Editors <managerdev-editors>` section.

.. _managerdev-mac:

Mac
^^^

Make sure you have ``watchman`` installed on your machine. You can do
this with homebrew:

.. code:: sh

   brew install watchman

Linux
^^^^^

Install these dependencies for cypress:

.. code:: sh

   yum install -y xorg-x11-server-Xvfb gtk2-2.24* libXtst* libXScrnSaver* GConf2* alsa-lib*

..

   Have issues while setting up? Try the :ref:`Having issues <managerdev-having-issues>` section to troubleshoot.

Development
-----------

.. note::
  If this is your first time running this project, you need to complete
  the :ref:`Initial machine setup <managerdev-initial-setup>` section above 👆

Quick start guide
^^^^^^^^^^^^^^^^^

  Make sure you’re at the Ohara manager root, then start it with:

  .. code:: sh

    yarn start --configurator http://host:port/v0

  ..

     Note that the configurator option is required, and you should have
     configurator running before starting Ohara manager. You can see the
     :ref:`user guide <userguide>` on how to spin up a configurator


  Open another terminal tab, and start the **Client**:

  ::

     yarn start:client

  Now, go to http://localhost:3000 and start your development, happy
  hacking 😎

Full development guide
^^^^^^^^^^^^^^^^^^^^^^

In development, you need to start both the **Ohara manager** and **Ohara
manager client** servers before you can start your development. Follow
the instructions below:

Server:

  Make sure you’re at the ohara-manager root:

  .. code:: sh

     yarn start --configurator http://host:port/v0

  .. note::
    Note that the ``--configurator`` argument is required, you should
    pass in Ohara configurator API URL.

  You can also override the default port ``5050`` by passing in ``--port``
  like the following:

  .. code:: sh

    yarn start --configurator http://host:port/v0 --port 1234

  After starting the server, visit ``http://localhost:${PORT}`` in your
  browser.

  Double check the configurator spelling and API URL, the URL should
  contain the API version number: ``/v0``

Client:

  Start the **Client** development server with:

  .. code:: sh

    yarn start:client

  After starting the dev server, visit ``http://localhost:3000`` in your browser and start you development.

  You can override the default port ``3000`` by passing in an environment variable:

  .. code:: sh

    PORT=7777 yarn start:client

  The dev server will then start at ``http://localhost:7777``

Test
^^^^

You can run both the **Server** and **Client** unit tests with a single
npm script:

.. code:: sh

   yarn test

You can also run them separately as:

Server:

  Make sure you’re in the ohara-manager root, and use the following commands:

  Run the test and stay in Jest watch mode

  .. code:: sh

    yarn test:watch

  Generate a test coverage report
  The coverage reports can be found in ``ohara-manager/coverage/``

  .. code:: sh

    yarn test:coverage

Client:

  Run the tests and stay in Jest’s watch mode

  .. code:: sh

    yarn test:client

  Generate test coverage reports
  The coverage reports can be found in ``ohara-manager/client/coverage/``

  .. code:: sh

    yarn test:client:coverage

  We also have a npm script that runs both the **client** server and unit tests together:

  .. code:: sh

    yarn dev:client

  **Client** also has End-to-End tests, you can run them via the following command:

  .. code:: sh

    yarn test:e2e:open

  This will open cypress test runner, you can then run your test manually through the UIs.

Linting
^^^^^^^

  We use `ESLint <https://github.com/eslint/eslint>`__ to lint all the JavaScript:

  Server:

    .. code:: sh

      yarn lint:server

    It’s usually helpful to run linting while developing and that’s included in ``yarn start`` command:

    .. code:: sh

      yarn start --configurator http://host:port/v0

    This will start the server with ``nodemon`` and run the linting script whenever nodemon reloads.

  Client:

    Since our client is bootstrapped with create-react-app, so the linting
    part is already taken care. When starting the **Client** dev server with
    ``yarn start:client``, the linting will be starting automatically.

    Note that due to create-react-app doesn’t support custom eslint rules.
    You need to use your text editor plugin to display the custom linting
    rule warnings or errors. For more info about this, please take a look at
    the create-react-app
    `docs <https://facebook.github.io/create-react-app/docs/setting-up-your-editor#displaying-lint-output-in-the-editor>`__


Format
^^^^^^

  We use `Prettier <https://github.com/prettier/prettier>`__ to format our
  code. You can format all ``.js`` files with:

  .. code:: sh

    yarn format

  - You can ignore files or folders when running ``yarn format`` by
    editing the ``.prettierignore`` in the Ohara-manager root.

  .. note::
   Note that ``node_modules`` is ignore by default so you don’t need to
   add that in the ``.prettierignore``


Build
^^^^^

  **Note that this step is only required for the Client NOT THE SERVER**

  You can get production-ready static files by using the following command:

  .. code:: sh

     yarn build

  .. note::
     These static files will be built and put into the **/ohara-manager/client/build** directory.


Ohara manager image
^^^^^^^^^^^^^^^^^^^

  Run the following command to get the production ready build of both the
  **Server** and **Client**.

  .. code:: sh

    yarn setup

  After the build, copy/use these files and directories to the destination
  directory (Note this step is automatically done by Ohara-assembly
  module):

  -  start.js
  -  config.js
  -  client – only build directory is needed

     -  build

  -  constants
  -  node_modules
  -  routes
  -  utils

  .. note::
     Note that if you add new files or dirs to the **Server** or
     **Client** and these files and dirs are required for production
     build, please list that file in the above list as well as editing the
     gradle file under ``ohara/ohara-assembly/build.gradle``. **Skipping
     this step will cause production build failed!**

  **From the Ohara manager project root**, use the following command to
  start the manager:

  .. code:: sh

    yarn start:prod --configurator http://host:port/v0


CI server integration
^^^^^^^^^^^^^^^^^^^^^

  In order to work with Graddle on Jenkins, Ohara manager provides a few
  npm scripts as the following:

  Run tests on CI:

  .. code:: sh

    yarn test

  -  Run all tests including the **Server** and the **Client** unit tests.
     The test reports can be found in ``ohara-manager/test-reports/``

  -  Note you should run ``yarn setup`` to ensure that all necessary
     packages are installed prior to running tests.


Clean
^^^^^

  Clean up all running processes, removing ``test-reports/`` in the
  **Server** and ``/build`` directory in the **Client**:

  .. code:: sh

    yarn clean

  Clean all running processes started with node.js

  .. code:: sh

    yarn clean:process

  This is useful when you want to kill all node.js processes

Prepush
^^^^^^^

  We also provide a npm script to run all the tests (both client and
  server unit tests and e2e tests) lint, and format all the JS files with.
  **Ideally, you’d run this before pushing your code to the remote repo:**

  .. code:: sh

    yarn prepush

.. _managerdev-editors:

Editors
-------

  We highly recommend that you use `Visual Studio Code <https://code.visualstudio.com/>`__ (or vscode for short) to edit
  and author Ohara manager code.

  **Recommend vscode settings**

  .. code:: json

     {
       "editor.tabSize": 2,
       "editor.formatOnSave": true,
       "editor.formatOnSaveTimeout": 2000,
       "editor.tabCompletion": true,
       "emmet.triggerExpansionOnTab": true,
       "emmet.includeLanguages": {
         "javascript": "javascriptreact",
         "markdown": "html"
       },
       "search.exclude": {
         "**/node_modules": true,
         "**/bower_components": true,
         "**/coverage": true
       },
       "prettier.eslintIntegration": true,
       "javascript.updateImportsOnFileMove.enabled": "always"
     }

  **Recommend extensions**

  -  `ESLint <https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint>`__
     - install this so vscode can display linting errors right in the
     editor
  -  `vscode-styled-components <https://marketplace.visualstudio.com/items?itemName=jpoissonnier.vscode-styled-components>`__
     - syntax highlighting support for `styled
     component <https://github.com/styled-components/styled-components>`__
  -  `Prettier - Code
     formatter <https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode>`__
     - code formatter, it consumes the config in ``.prettierrc``
  -  `DotENV <https://marketplace.visualstudio.com/items?itemName=mikestead.dotenv>`__
     - ``.env`` file syntax highlighting support
  -  `Color
     Highlight <https://marketplace.visualstudio.com/items?itemName=naumovs.color-highlight>`__
     - Highlight web colors in VSCode


Switch different version of Node.js
-----------------------------------

  Oftentimes you would need to switch between different Node.js versions
  for debugging. There’s a handy npm package that can reduce the pain of
  managing different version of Node.js on your machine:

  First, let’s install this package ``n``

  .. code:: sh

     # install this globally so it's can be used through out all your projects
     npm install -g n # or yarn global add n

  Second, let’s use ``n`` to install a specific version of Node.js:

  .. code:: sh

    n 8.16.0

  .. note::
     After the specific version is installed, ``n`` will switch your
     active Node.js version to it

  You can switch between versions that you have previously installed on
  your machine with ``n``, an interactive prompt will be displayed and you
  can easily choose a Node.js version form it

  .. code:: sh

    n # Yep, just type n in your terminal...,

  For more info, you can read the `docs <https://github.com/tj/n>`__ here.

.. _managerdev-having-issues:

Having issues?
--------------

  -  **Got an error while starting up the server: Error: Cannot find module ${module-name}**

     If you’re running into this, it’s probably that this module is not
     correctly installed on your machine. You can fix this by simply run:

     .. code:: sh

         yarn # If this doesn't work, try `yarn add ${module-name}`

     After the installation is completed, start the server again.

  -  **Got an error while starting up the server or client on a Linux machine: ENOSPC**

     You can run this command to increase the limit on the number of files
     Linux will watch. Read more
     `here <https://github.com/guard/listen/wiki/Increasing-the-amount-of-inotify-watchers#the-technical-details>`__.

     .. code:: sh

        echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf && sudo sysctl -p.

  -  **Node.js processes cannot be stopped even after using kill -9**

     We’re using ``forever`` to start our node.js servers on CI, and
     ``nodemon`` while in development, so you need to use the following
     commands to kill them. ``kill -9`` or ``fuser`` might not work as you
     expected.

     use ``yarn clean:processes`` command or ``pkill node`` to kill all the node.js processes

  -  **While running test in jest’s watch modal, an error is thrown**

     ::

        Error watching file for changes: EMFILE

     Try installing ``watchman`` for your mac with the :ref:`instruction <managerdev-mac>`

     For more info: https://github.com/facebook/jest/issues/1767

  -  **Ohara manager is not able to connect to Configurator**

     And I’m seeing something like:

     ::

        --configurator: we're not able to connect to http://host:port/v0

        Please make sure your Configurator is running at http://host:port/v0

        [nodemon] app crashed - waiting for file changes before starting...

     This could happen due to several factors:

     -  **Configurator hasn’t fully started yet**: after you start the
        configurator container. The container needs some time to fully
        initialize the service. This usually takes about a minute or so.
        And as we’re doing the API check by hitting the real API in Ohara
        manager. This results to the error in the above.

     -  **You’re not using the correct IP in Manager container**: if you
        start a configurator container in your local as well as a manager.
        You should specify an IP instead of something like localhost in:
        –configurator http://localhost:12345/v0 This won’t work as the
        manager is started in the container so it won’t be able to connect
        to the configurator without a real IP

     -  **As we mentioned in the previous sections. Please double check
        your configurator URL spelling. This is usually the cause of the
        above-mentioned error**
