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

.. _quick-rst:

reStructuredText quick example
==============================

.. note::
  This page is **not** a reStructuredText Markup tutorial. If you are looking for tutorial or more detail about
  reStructuredText, here are some reference links:

  * `A ReStructuredText Primer <http://docutils.sourceforge.net/docs/user/rst/quickstart.html>`_
  * `Quick reStructuredText <http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_
  * `Markup Specification <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#sections>`_
  * https://thomas-cokelaer.info/tutorials/sphinx/rest_syntax.html
  * [中文] `reStructuredText 簡介 <https://zh-sphinx-doc.readthedocs.io/en/latest/rest.html>`_
  * [中文] `reStructuredText 学习笔记 <http://notes.tanchuanqi.com/tools/reStructuredText.html>`_

Basic
---------

Section Structure
^^^^^^^^^^^^^^^^^

An underline/overline is a single repeated punctuation character that begins in column 1 and forms a line extending at
least as far as the right edge of the title text.

Plain text example
  .. code-block:: text

   Level 1
   ===========

   Level 2
   -----------

   Level 3
   ^^^^^^^^^^^

   Level 4
   """""""""""


- ``=`` --> Title level 1, Page title
- ``-`` --> Title level 2, Section heading
- ``^`` --> Title level 3, Sub section heading
- ``"`` --> Title level 4, Sub sub-section heading

  .. note::
    Please use the list above as section underline character in Ohara.


Inline Markup
^^^^^^^^^^^^^^^

Plain text
  .. code-block:: text

    * *emphasis*
    * **strong emphasis**
    * ``inline literal``
    * Partial character emphasis: Collection\ **s**
    * Substitution: |ohara-intro|

      * please see `Substitution section <#quick-example-substitution>`_

Result
  * *emphasis*
  * **strong emphasis**
  * ``inline literal``
  * Partial character emphasis: Collection\ **s**
  * Substitution: |ohara-intro|

    * please see `Substitution section <#quick-example-substitution>`_


Tables
------

ReStructuredText provides two syntaxes for delineating table cells: **Grid Tables** and **Simple Tables**.

Simple Tables
^^^^^^^^^^^^^

Plain text sample 1
  .. code-block:: text

    =====  =====  =======
      A      B    A and B
    =====  =====  =======
    False  False  False
    True   False  False
    True   True   True
    =====  =====  =======

Result
  =====  =====  =======
    A      B    A and B
  =====  =====  =======
  False  False  False
  True   False  False
  True   True   True
  =====  =====  =======

Plain text sample 2
  .. code-block:: text

    =====  =====  ======
       Inputs     Output
    ------------  ------
      A      B    A or B
    =====  =====  ======
    False  False  False
    True   False  True
    True   True   True
    =====  =====  ======

Result
  =====  =====  ======
     Inputs     Output
  ------------  ------
    A      B    A or B
  =====  =====  ======
  False  False  False
  True   False  True
  True   True   True
  =====  =====  ======

Plain text sample 3
  .. code-block:: text

    =====  =====
    col 1  col 2
    =====  =====
    1      Second column of row 1.
    2      Second column of row 2.
           Second line of paragraph.
    3      - Second column of row 3.

           - Second item in bullet
             list (row 3, column 2).
    \      Row 4; column 1 will be empty.
    =====  =====

Result
  =====  =====
  col 1  col 2
  =====  =====
  1      Second column of row 1.
  2      Second column of row 2.
         Second line of paragraph.
  3      - Second column of row 3.

         - Second item in bullet
           list (row 3, column 2).
  \      Row 4; column 1 will be empty.
  =====  =====

Reference for detail:
  - `Simple Tables <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#simple-tables>`_


Grid Tables
^^^^^^^^^^^

Plain text sample 1
  .. code-block:: text

    +------------------------+------------+----------+----------+
    | Header row, column 1   | Header 2   | Header 3 | Header 4 |
    | (header rows optional) |            |          |          |
    +========================+============+==========+==========+
    | body row 1, column 1   | column 2   | column 3 | column 4 |
    +------------------------+------------+----------+----------+
    | body row 2             | Cells may span columns.          |
    +------------------------+------------+---------------------+
    | body row 3             | Cells may  | - Table cells       |
    +------------------------+ span rows. | - contain           |
    | body row 4             |            | - body elements.    |
    +------------------------+------------+---------------------+

Result
  +------------------------+------------+----------+----------+
  | Header row, column 1   | Header 2   | Header 3 | Header 4 |
  | (header rows optional) |            |          |          |
  +========================+============+==========+==========+
  | body row 1, column 1   | column 2   | column 3 | column 4 |
  +------------------------+------------+----------+----------+
  | body row 2             | Cells may span columns.          |
  +------------------------+------------+---------------------+
  | body row 3             | Cells may  | - Table cells       |
  +------------------------+ span rows. | - contain           |
  | body row 4             |            | - body elements.    |
  +------------------------+------------+---------------------+


Reference for detail:
  - `Grid Tables <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#grid-tables>`_


List
----

Bullet Lists
^^^^^^^^^^^^

Plain text example
  .. code-block:: text

    - This is the first bullet list item.  The blank line above the
      first list item is required; blank lines between list items
      (such as below this paragraph) are optional.

    - This is the first paragraph in the second item in the list.

      This is the second paragraph in the second item in the list.
      The blank line above this paragraph is required.  The left edge
      of this paragraph lines up with the paragraph above, both
      indented relative to the bullet.

      - This is a sublist.  The bullet lines up with the left edge of
        the text blocks above.  A sublist is a new list so requires a
        blank line above and below.

    - This is the third item of the main list.

    This paragraph is not part of the list.


Result
  - This is the first bullet list item.  The blank line above the
    first list item is required; blank lines between list items
    (such as below this paragraph) are optional.

  - This is the first paragraph in the second item in the list.

    This is the second paragraph in the second item in the list.
    The blank line above this paragraph is required.  The left edge
    of this paragraph lines up with the paragraph above, both
    indented relative to the bullet.

    - This is a sublist.  The bullet lines up with the left edge of
      the text blocks above.  A sublist is a new list so requires a
      blank line above and below.

  - This is the third item of the main list.

  This paragraph is not part of the list.


Enumerated Lists
^^^^^^^^^^^^^^^^

Plain text example
  .. code-block:: text

    #. Item 1 initial text.

       a) Item 1a.
       b) Item 1b.

    #. a) Item 2a.
       b) Item 2b.

Result
  #. Item 1 initial text.

     a) Item 1a.
     b) Item 1b.

  #. a) Item 2a.
     b) Item 2b.



Definition Lists
^^^^^^^^^^^^^^^^

Plain text example
  .. code-block:: text

    term 1
        Definition 1.

    term 2
        Definition 2, paragraph 1.

        Definition 2, paragraph 2.

    term 3 : classifier
        Definition 3.

    term 4 : classifier one : classifier two
        Definition 4.

Result
  term 1
      Definition 1.

  term 2
      Definition 2, paragraph 1.

      Definition 2, paragraph 2.

  term 3 : classifier
      Definition 3.

  term 4 : classifier one : classifier two
      Definition 4.



Field Lists
^^^^^^^^^^^

Plain text example
  .. code-block:: text

    :Date: 2001-08-16
    :Version: 1
    :Authors: - Me
              - Myself
              - I
    :Indentation: Since the field marker may be quite long, the second
       and subsequent lines of the field body do not have to line up
       with the first line, but they must be indented relative to the
       field name marker, and they must line up with each other.
    :Parameter i: integer


Result
  :Date: 2001-08-16
  :Version: 1
  :Authors: - Me
            - Myself
            - I
  :Indentation: Since the field marker may be quite long, the second
     and subsequent lines of the field body do not have to line up
     with the first line, but they must be indented relative to the
     field name marker, and they must line up with each other.
  :Parameter i: integer

..
.. TODO: more Blocks example
..

Hyperlinks
----------

External hyperlink targets
^^^^^^^^^^^^^^^^^^^^^^^^^^

Plain text example
    .. code-block:: text

      - External hyperlink: `<https://ohara.readthedocs.io/>`_
      - External hyperlink: https://ohara.readthedocs.io/
      - External hyperlink: `Apache Kafka Github Repo <https://github.com/apache/kafka>`_
      - External hyperlink: Ohara_
      - External hyperlink: `Ohara Document`_

      .. _Ohara: https://github.com/oharastream/ohara
      .. _Ohara Document: https://ohara.readthedocs.io/en/latest/

Result
    - External hyperlink: `<https://ohara.readthedocs.io/>`_
    - External hyperlink: https://ohara.readthedocs.io/
    - External hyperlink: `Apache Kafka Github Repo <https://github.com/apache/kafka>`_
    - External hyperlink: Ohara_
    - External hyperlink: `Ohara Document`_

    .. _Ohara: https://github.com/oharastream/ohara
    .. _Ohara Document: https://ohara.readthedocs.io/en/latest/


.. _quick-example-internal-targets:

Internal hyperlink targets
^^^^^^^^^^^^^^^^^^^^^^^^^^

Internal hyperlink targets is standard reST label, must be **unique** throughout the entire documentation.

Plain text example
  .. code-block:: text

    .. _quick-rst-internal-targets:

    - Goto :ref:`Internal hyperlink targets <quick-example-internal-targets>`
    - Goto quick-example-internal-targets_
    - Goto `Internal hyperlink targets <#quick-example-internal-targets>`_

Result
    - Goto :ref:`Internal hyperlink targets <quick-example-internal-targets>`
    - Goto quick-example-internal-targets_
    - Goto `Internal hyperlink targets <#quick-example-internal-targets>`_


.. note::
  We can use `:ref: <https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#cross-referencing-arbitrary-locations>`_
  to cross-referencing arbitrary locations in any document.

.. warning::
  Please don't point the **implicit hyperlink targets** that auto generated by section titles.
  Use **internal hyperlink targets** instead to keep the internal link not broken.

Reference for detail:
  - `Hyperlink Targets <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#hyperlink-targets>`_
  - `Hyperlink References <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#hyperlink-references>`_
  - `Cross-referencing anything <https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#cross-referencing-arbitrary-locations>`_


Custom external links in Ohara
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ohara enable `sphinx.ext.extlinks`_ to help with common pattern to point to the external sites.
Following extlinks are define in **docs/src/conf.py**:

  .. code-block:: python

    extlinks = {
        'ohara-issue': ('https://github.com/oharastream/ohara/issues/%s', '#'),
        'ohara-source': ('https://github.com/oharastream/ohara/blob/%s/' % ohara_branch + "%s", ''),
        'kafka-issue': ('https://issues.apache.org/jira/browse/KAFKA-%s', 'KAFKA-'),
        'zookeeper-issue': ('https://issues.apache.org/jira/browse/ZOOKEEPER-%s', 'ZOOKEEPER-'),
        'k8s-issue': ('https://github.com/kubernetes/kubernetes/issues/%s', '#')
    }

Plain text example
  .. code-block:: text

    - Ohara issue link: :ohara-issue:`800`
    - Ohara source link: :ohara-source:`PageViewRegionExample <ohara-streams/src/test/java/com/island/ohara/streams/examples/PageViewRegionExample.java>`
    - Kafka issue: :kafka-issue:`8765`
    - Zookeeper issue: :zookeeper-issue:`2345`
    - Kubernetes issue: :k8s-issue:`2345`

Result
  - Ohara issue link: :ohara-issue:`800`
  - Ohara source link: :ohara-source:`PageViewRegionExample <ohara-streams/src/test/java/com/island/ohara/streams/examples/PageViewRegionExample.java>`
  - Kafka issue: :kafka-issue:`8765`
  - Zookeeper issue: :zookeeper-issue:`2345`
  - Kubernetes issue: :k8s-issue:`2345`


.. note::
  The extlink ``:ohara-source`` also bind *branch* variable to make sure that we can point to
  the correct url.

.. _sphinx.ext.extlinks: https://www.sphinx-doc.org/en/master/usage/extensions/extlinks.html#module-sphinx.ext.extlinks

.. _quick-rst-substitution:

Substitution
------------

Substitution definition
^^^^^^^^^^^^^^^^^^^^^^^

Plain text example
  .. code-block:: text

    - About Ohara: |ohara-intro|
    - The |happy-face| symbol must be used on containers used to dispose of medical waste.

Result
  - About Ohara: |ohara-intro|
  - The |happy-face| symbol must be used on containers used to dispose of medical waste.

.. |ohara-intro| replace:: Ohara, a big data platform which is powered by Apache Kafka and Docker, enables effective and quick ways of building application at streaming and scale.
.. |happy-face| image:: https://cdn4.iconfinder.com/data/icons/emoji-18/61/2-32.png

Reference for detail:
  - `Substitution Definitions <http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html#substitution-definitions>`_
  - `Replacement Text <http://docutils.sourceforge.net/docs/ref/rst/directives.html#replacement-text>`_


Predefine substitution definition
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are two important substitution definitions: **version**, **branch**. The two values are read from
*gradle.properties* when you build ohara docs.

- **version**: Used to indicate which release version when Ohara released.
- **branch**: Used to indicate which github repo branch the source code belongs to.

Plain text example
  .. code-block:: text

    - Ohara version: |version|
    - Ohara branch: |branch|

Result
  - Ohara version: |version|
  - Ohara branch: |branch|

If you want to use it in code-block, you should add a prefix character ``$``, for example:

- $\|version|
- $\|branch|

Code-block example
  .. code-block:: groovy

     repositories {
          maven {
              url "https://dl.bintray.com/oharastream/ohara"
          }
      }
     implementation "com.island.ohara:ohara-common:$|version|"
     implementation "com.island.ohara:ohara-kafka:$|version|"


Show code
---------

Java block
  .. code-block:: java

     public class SimpleApplicationForOharaEnv extends StreamApp {

       @Override
       public void start() {
         OStream<Row> ostream = OStream.builder().cleanStart().toOharaEnvStream();
         ostream.start();
       }
     }

Console block
  .. code-block:: console

    $ curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
    $ python get-pip.py --user

Json block
  .. code-block:: json

     {
       "name": "aa.jar",
       "group": "wk01",
       "size": 1779,
       "url": "http://localhost:12345/v0/downloadFiles/aa.jar",
       "lastModified": 1561012496975
     }



Include file
^^^^^^^^^^^^

Plain text sample 1
  .. code-block:: text

    .. literalinclude:: _include/sample.json
      :language: json

Result
  .. literalinclude:: _include/sample.json
    :language: json

Plain text sample 1
  .. code-block:: text

    .. literalinclude:: _include/RowSourceConnector.java
      :language: java

Result
  .. literalinclude:: _include/RowSourceConnector.java
    :language: java


Reference for detail:
  - `Showing code examples <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#showing-code-examples>`_
      - `code-block <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-code-block>`_
      - `literalinclude <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#directive-literalinclude>`_

Paragraph-level markup
-----------------------

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

  .. deprecated:: 0.6
    Use xxx instead.

  .. versionadded:: 0.7
    The *spam* parameter.

  .. code-block:: console

    pandoc --from=markdown --to=rst --output=README.rst README.md

  .. hlist::
   :columns: 3

   * A list of
   * short items
   * that should be
   * displayed
   * horizontally

Reference:
  - `Paragraph-level markup <https://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#paragraph-level-markup>`_


Other reference
---------------

- https://docs.typo3.org/
- https://developer.lsst.io/index.html
