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

A sampe file for rst practice
==============================

- Ohara version: |version|
- Ohara branch: |branch|
- Ohara issue link: :ohara-issue:`800`
- Ohara source link: :ohara-source:`PageViewRegionExample <ohara-streams/src/test/java/com/island/ohara/streams/examples/PageViewRegionExample.java>`
- Kafka issue: :kafka-issue:`8765`
- Zookeeper issue: :zookeeper-issue:`2345`
- Kubernetes issue: :k8s-issue:`2345`

.. _my_label:

Replace version and branch
--------------------------

This document branch is: |branch|

We can replace **$\|VERSION\|** in the code block:

.. code:: groovy

   repositories {
        maven {
            url "https://dl.bintray.com/oharastream/ohara"
        }
    }
   implementation "com.island.ohara:ohara-common:$|version|"
   implementation "com.island.ohara:ohara-kafka:$|version|"



Source code link: `WordCountExample`_

.. _WordCountExample: https://github.com/oharastream/ohara/blob/$|branch|/ohara-streams/src/test/java/com/island/ohara/streams/examples/WordCountExample.java


.. note::
  This is note

.. danger::
  This is danger

.. tip::
  This is tip

.. warning::
  This is warning

.. option::
  This is option

.. seealso::
  This is see also

.. code-block:: console

  pandoc --from=markdown --to=rst --output=README.rst README.md


This is a link my_label_

This is a link :ref:`my_label`
