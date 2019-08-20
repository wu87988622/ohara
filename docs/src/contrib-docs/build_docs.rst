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

How to build Ohara docs
=======================


Prerequisites
-------------

* Python 2.7+

.. note::
  Currently, we are not using Pyhton 3.x. We would upgrade before 2020.


Installation
------------

Pip
^^^

  You must install `Pip <https://pip.pypa.io/>`_ before you install Sphinx.
  Please follow the `here <https://pip.pypa.io/en/stable/installing/>`_
  to install.

  .. warning::
    Please use Pip, do not use Pip3

  .. code-block:: console

    $ curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
    $ python get-pip.py --user

  .. note::
    We recommend to use ``--user`` to install pip to avoid **Permisson denined** issue.

  After successful pip installation, you can type following to look version:

  .. code-block:: console

    $ pip --version
    pip 19.2.1 from /home/XXXX/.local/lib/python2.7/site-packages/pip (python 2.7)


Sphinx
^^^^^^^

  After installed Pip, now you can install Sphinx:

  .. code-block:: console

    $ pip install --user --upgrade Sphinx

  .. note::
    We recommend to use ``--user`` to install pip to avoid **Permisson denined** issue.


  Sphinx provide some command-line tools. You can try the following command:

  .. code-block:: console

    $ sphinx-build --help

Other modules
^^^^^^^^^^^^^

  .. code-block:: console

    $ pip install --user recommonmark
    $ pip install --user sphinx_rtd_theme


  Now you can build Ohara document through Sphinx.


Build docs
----------------

  You can use `make` or `sphinx-build` to generate the html files:

  .. code-block:: console

    [ohara]$ cd docs
    [docs]$ make html

  OR

  .. code-block:: console

    [ohara]$ cd docs
    [docs]$ sphinx-build -M html src/ build/

  The screen output would be like following:

  .. code-block:: console

    [docs]$ make html
    Running Sphinx v1.8.5
    ========================================
    Ohara version: $|version|
    Ohara branch/tag: master
    Producton Mode: False
    ========================================
    loading pickled environment... done
    building [mo]: targets for 0 po files that are out of date
    building [html]: targets for 1 source files that are out of date
    updating environment: [] 0 added, 1 changed, 0 removed
    reading sources... [100%] contrib-docs/build_docs
    looking for now-outdated files... none found
    pickling environment... done
    checking consistency... done
    preparing documents... done
    writing output... [100%] index
    generating indices... genindex
    writing additional pages... search
    copying images... [100%] images/ohara-logo.png
    copying static files... done
    copying extra files... done
    dumping search index in English (code: en) ... done
    dumping object inventory... done
    build succeeded.

    The HTML pages are in build/html.
    [docs]$

  All html files output to `build` folder. Now you can open browser and open the file: ``file:///${OHARA_ROOT}/docs/build/html/index.html``

  If you want to clean **build** folder, just type: ``$ make clean``


Quick Preview .rst file
-----------------------

If you need an quick preview tool while writing the doc,
we recommand `restview <https://mg.pov.lt/restview/>`_.

