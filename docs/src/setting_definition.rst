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

.. _setting-definition:

Setting Definition Guide
========================

A powerful application always has a complicated configuration. In order to
be a best friend of Ohara users, Ohara provides a method which
can return the details of setting definitions, and ohara suggests that
all developers ought to implement the method so as to guide
users through the complicated settings of your applications.

.. note::
   If you have no interest in settings or your application is too simple
   to have any settings, you can just skip this section.

SettingDef is a class used to describe the details of **a** setting. It consists of following arguments.

#. :ref:`reference <setting-definition-reference>` (**string**) — works for ohara manager. It represents the reference of value.
#. group (**string**) — the group of this setting (all core setting are in core group)
#. orderInGroup (**int**) — the order in group
#. displayName (**string**) — the readable name of this setting
#. editable (**boolean**) — true if this setting is modifiable
#. key (**string**) — the key of configuration
#. :ref:`valueType <setting-definition-type>` (**string**) — the type of value
#. required (**boolean**) — true if this setting has no default value and you have to assign a value. Otherwise, you can’t start connector.
#. defaultValue (**string**) — the default value
#. documentation (**string**) — the explanation of this definition
#. internal (**string**) — true if this setting is assigned by system automatically.
#. tableKeys (**array(string)**) — the column name when the type is TABLE

.. note::
   You can call :ref:`Worker APIs <rest-workers>` to get all connectors’ setting definitions, and use
   :ref:`Stream APIs <rest-stream>` to get all streamApp setting definitions.

Although a SettingDef can include many elements, you can simply
build a SettingDef with only what you need. An extreme example is
that you can create a SettingDef with only key.

.. code-block:: java

   SettingDef.builder().key(key).build();

Notwithstanding it is flexible to build a SettingDef, we
encourage developers to create a description-rich
SettingDef. More description to your setting produces more
**document** in calling ohara rest APIs. We
all hate write documentation so it would be better to make your code
readable.


Reference, Internal and TableKeys Are NOT Public
------------------------------------------------

Ohara offers a great UI, which is located at ohara-manager. The UI
requires some **private** information to generate forms for custom
applications. The such private information is specific-purpose and is
meaningless to non-ohara developers. Hence, all of them are declared as
package-private and ohara does not encourage developers
to stop at nothing to use them.


Optional, Required And Default Value
------------------------------------

We know a great application must have countless settings and only The
Chosen One can control it. In order to shorten the gap between your
application and human begin, ohara encourage developers
to offer the default values to most of settings as much as possible.
Assigning a default value to a SettingDef is a piece of cake.

.. code-block:: java

   SettingDef.builder().key(key).optional(defaultValue).build();

.. note::
   the default value is declared as **string** type as it must be **readable** in Restful APIs.

After calling the **optional(String)** method, the response, created by
:ref:`Worker APIs <rest-workers>` for example, will display
the following information.

.. code-block:: json

   {
     "required": false,
     "defaultValue": "ur_default_value"
   }

.. note::
   The default value will be added to :ref:`TaskSetting <connector-source-start>` automatically if the specified
   key is not already associated with a value.

A Readonly Setting Definition
-----------------------------

You can declare a **readonly** setting that not only exposes something
of your application to user but also remind user the setting can’t be
changed at runtime. For instance, the information of
:ref:`version <connector-version>` is fixed after you have completed your connector
so it is not an **editable** setting. Hence, ohara define a setting for
**version** with a readonly label. By the way, you should assign a
default value to a readonly setting since a readonly setting without
default value is really weird. There is a example of creating a readonly
setting.

.. code-block:: java

   SettingDef.builder().key(key).optional(defaultValue).readonly().build();

.. note::
   The input value will be removed automatically if the associated setting is declared readonly.

.. _setting-definition-reference:

Setting Reference
-----------------

This element is a specific purpose. It is used by Ohara manager (UI)
only. If you don’t have interest in UI, you can just ignore this
element. However, we still list the available values here.

#. TOPIC
#. WORKER_CLUSTER

.. note::
   For each reference value, it may has different type and will produce different behavior.

Topic String
   .. code-block:: java

      SettingDef.builder().key("topic").reference(Reference.TOPIC).valueType(Type.STRING).build();

   which means the request should "accept one topic of string type"

   .. code-block:: json

      {
        "topic": "t1"
      }

----------------------------

TopicKey List
   .. code-block:: java

      SettingDef.builder().key("topicKeys").reference(Reference.TOPIC).valueType(Type.TOPIC_KEYS).build();

   which means the request should "accept topic list of **TopicKey** type"

   .. code-block:: json

      {
        "topicKeys": [
          {
            "group": "default",
            "name": "t1"
          },
          {
            "group": "default",
            "name": "t2"
          }
        ]
      }

----------------------------

Topic String List
   .. code-block:: java

      SettingDef.builder().key("topics").reference(Reference.TOPIC).valueType(Type.ARRAY).build();

   which means the request should "accept topic list of string type"

   .. code-block:: json

      {
        "topics": ["t1", "t2", "t3"]
      }

.. _setting-definition-type:

Value Type
----------

In a custom application, the settings could have various data type. In order to display correct data type in
ohara manager and leverage the benefit of :ref:`type checker<checker>`, we strongly suggest you to define the
correct data type for each setting.

The following data types are supported currently.

Type.BOOLEAN
^^^^^^^^^^^^

Boolean type represents that the data should have only two possible value: **true** or **false**.
The value must be able cast to **java.lang.Boolean**

Type.STRING
^^^^^^^^^^^

String type represents that the data should be a string.
The value must be able cast to **java.lang.String**

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.STRING).build();

Type.SHORT
^^^^^^^^^^

Short type represents that the data should be a 2-bytes integer.
The value must be able cast to **java.lang.Short**

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.SHORT).build();

Type.INT
^^^^^^^^

Int type represents that the data should be a 4-bytes integer.
The value must be able cast to **java.lang.Integer**

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.INT).build();

Type.LONG
^^^^^^^^^

Long type represents that the data should be a 8-bytes integer.
The value must be able cast to **java.lang.Long**

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.LONG).build();

Type.DOUBLE
^^^^^^^^^^^

Double type represents that the data should be a 8-bytes floating point.
The value must be able cast to **java.lang.Double**

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.DOUBLE).build();

Type.ARRAY
^^^^^^^^^^

Array type represents that the data should be a collection of data.
We don't check the element data type in the collection, that is, the following request is legal in
SettingDef but will produce a weird behavior in ohara manager. We suggest you use the same data type of element
in array.


.. code-block:: json

   {
     "key": ["abc", 123, 2.0]
   }

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.ARRAY).build();

.. note::
   An empty array is ok and will pass the checker:

   .. code-block:: json

      {
        "key": []
      }

Type.CLASS
^^^^^^^^^^

Class type represents that the data is a class. This data type is used to display a value that is a class.
The value must be able cast to **java.lang.String**.

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.CLASS).build();

Type.PASSWORD
^^^^^^^^^^^^^

Password type represents that the data is a password. We will replace the value by **hidden** symbol in APIs.
if the data type is used as password.
The value must be able cast to **java.lang.String**.

.. code-block:: java

   SettingDef.builder().key(key).valueType(Type.PASSWORD).build();


Type.JDBC_TABLE
^^^^^^^^^^^^^^^

JDBC_TABLE is a specific string type used to reminder Ohara Manager that
this field requires a **magic** button to show available tables of
remote database via Query APIs. Except for the **magic** in UI, there is
no other stuff for this JDBC_TYPE since kafka can’t verify the input
arguments according to other arguments. It means we can’t connect to
remote database to check the existence of input table.

It is ok to replace this field by Type.STRING if you don’t use Ohara
Manager. Nevertheless, we still encourage developer to choose
the **fitting** type for your setting if you demand your user to input a
database table.

Type.TABLE
^^^^^^^^^^

Table type enable you to define a setting having table structure value.
Apart from assigning Type.Table to your setting definition, you also
have to define which keys are in your table. The following example show
a case that declares a table having two columns called **c0** and **c1**.

.. code-block:: java

   SettingDef.builder().key(key).tableKeys(Arrays.asList("c0", "c1")).valueType(Type.TABLE).build();

The legal value for above setting definition is shown below.

.. code-block:: json

   {
     "key": [
       {
         "c0": "v0",
         "c1": "v1"
       },
       {
         "c0": "v2",
         "c1": "v3"
       }
     ]
   }

The above example implies there is a table having two columns called
**c0** and **c1**. Also, you assign two values to **c0** that first is
**v0** and another is **v2**. Ohara offers a check for Type.Table that
the input value **must** match all keys in.

How to get the description of above **keys** ? If the setting type is
**table**, the setting must have **tableKeys**. It is a array of string
which shows the keys used in the table type. For instance, a setting
having table type is shown below.

.. code-block:: json

   {
     "reference": "NONE",
     "displayName": "columns",
     "internal": false,
     "documentation": "output schema",
     "valueType": "TABLE",
     "tableKeys": [
       "order",
       "dataType",
       "name",
       "newName"
     ],
     "orderInGroup": 6,
     "key": "columns",
     "required": false,
     "defaultValue": null,
     "group": "core",
     "editable": true
   }

.. note::
   If you ignore the table keys for Type.Table, the check to your input
   value is also ignored. By contrast, the table keys are useless for
   other types.

Type.DURATION
^^^^^^^^^^^^^

The time-based amount of time is a common setting in our world. However,
it is also hard to reach the consensus about the **string representation**
for a duration. For instance, the java.time.Duration
prefers ISO-8601, such as PT10S. The scala.concurrent.duration.Duration
prefers simple format, such as 10 seconds. Ohara offers a official
support to Duration type so as to ease the pain of using string in
connector. When you declare a setting with duration type, ohara provides
the default check which casts input value to java Duration and scala
Duration. Also, your connector can get the **Duration** from
:ref:`TaskSetting <connector-source-start>` easily without worrying about the
conversion between java and scala. Furthermore, connector users can
input both java.Duration and scala.Duration when starting connector.

The value must be castable to **java.time.Duration** and it is based on the ISO-860 duration
format PnDTnHnMn.nS

Type.PORT
^^^^^^^^^

Port is a common property to connector. For example, the ftp connector
needs port used to connect to source/target ftp server. Inputting a
illegal port can destroy connector easily. Declaring your type of value
to Port involve a check that only the port which is small than 65536 and
bigger than zero can be accepted. Other port value will be rejected in
starting connector.

Type.BINDING_PORT
^^^^^^^^^^^^^^^^^

This type is similar to Type.PORT except that the value mapped to BINDING_PORT has a extra check to the availability on
the target nodes. For example, you define value 5555 as a BINDING_PORT, and you will get a exception when you try to
deploy your code on the node which is using port 5555 as well.

Type.JAR_KEY
^^^^^^^^^^^^

Jar key represents a format of **com.island.ohara.common.setting.ObjectKey** for specific jar.
It consists "group" and "name" fields.
In a custom application, you should check the request contains both fields.

.. code-block::json

   {
     "jarKey": {
       "group": "default",
       "name": "abc.jar"
     }
   }

Type.TOPIC_KEYS
^^^^^^^^^^^^^^^

Topic key represents a format of **com.island.ohara.common.setting.TopicKey** for ohara topics.
It consists "group" and "name" fields.
Note the type of the plural char "s". It means the request value should pass a array.

.. code-block::json

   {
     "topicKeys": [{
       "group": "default",
       "name": "t1"
     }]
   }

Type.TAGS
^^^^^^^^^

Tags is a flexible type that accept a json object. It could uses in some circumstances that user needs to define
additional values which type is not list above.

.. code-block::json

   {
     "tags": {
       "name": "hello",
       "anArray": ["bar", "foo"],
       "count": 10,
       "params": {
         "k": "v"
       }
     }
   }

.. _checker:

Checker
-------

We all love quick failure, right? A quick failure can save our resource
and time. Ohara offers many checks for your setting according to the
**expected** type. For example, a setting declared **Duration** type has
a checker which validate whether the input value is able to be cast to
either java.time.Duration or scala.duration.Duration. However, you are
going to design a complicated connector which has specific limit for
input value.
