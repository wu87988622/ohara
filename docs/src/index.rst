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

.. OharaStream documentation master file

.. image:: images/ohara-logo.png
   :width: 150pt
   :align: right

Documentation
=============

OharaStream is open to users, developers and man living in earth. Hence, we prepare a bunch of docs to help you to
understand ohara comprehensively but quickly.


For user
--------

This section is for the ohara users who are going to install and then test official streaming application. For this case,
you don't need to compile, build or write anything for ohara. All you have to read the `User Guide`_
and then follow the advice of `installation section`_.

.. _User Guide: user_guide.html
.. _installation section: user_guide.html#installation


For Developer
-------------

If you want to know how to build ohara, please read: `how to build`_

Apart from contributing code to ohara, you can also leverage ohara to design your `custom connector`_,
`custom streamapp` or build your UI interface via `Ohara REST interface`_.

.. _how to build: how_to_build.html
.. _custom connector: custom_connector.html
.. _custom streamapp: custom_streamapp.html
.. _Ohara REST interface: rest_interface.html


.. toctree::
   :maxdepth: 2
   :caption: Table of Contents

   user_guide
   rest_interface
   custom_connector
   custom_streamapp
   how_to_build
   ohara_manager_development_guideline
   docker
   integration_test
   contributing

Indices and tables
==================

* :ref:`genindex`
* :ref:`search`
