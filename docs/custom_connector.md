# custom connector guideline

Ohara custom connector is based on [kafka connector](https://docs.confluent.io/current/connect/managing/index.html).
It offers a platform that enables you to define some simple actions to connect topic to any other system.
You don't need to worry the application availability, data durability, or distribution anymore. All you have to do is to
write your custom connector, which can have only the pull()/push() method, and then compile your code to a jar file.
After uploading your jar to ohara, you are able to **deploy** your  connector on the
[worker cluster](rest_interface.md#create-a-worker-cluster). By leveraging ohara connector framework, apart from the availability,
scalability, and durability, you can also monitor your connector for [logs](rest_interface.md#logs) and [metrics](#metrics). 

The following sections start to explain how to write a good connector on ohara. You don't need to read it through if you
are familiar with [kafka connector](https://docs.confluent.io/current/connect/managing/index.html). However, ohara connector has some
improvements which are not in [kafka connector](https://docs.confluent.io/current/connect/managing/index.html) so it has
worth of taking a look at [metrics](#metrics) and [setting definitions](#setting-definitions.)

----------
## ohara connector overview

Ohara connector is composed of [source connect](#source-connector) and [sink connector](#sink-connector). 
[source connect](#source-connector) is used to pull data **from external system to topic**. By contrast, [sink connector](#sink-connector)
is used to pull data from **topic to external system**. A complete connector consists of **SourceConnector**/
**SinkConnector** and **SourceTask**/**SinkTask**. Worker cluster picks up a node to host your source/sink
connector and then distributes the source/sink tasks across cluster. 

### data model

Ohara has defined a table structure data in code base. We call it **row**. A row is comprised of multiple **cells**. Each cell has
its **name**, **value** and **tags**. The value in cell is a generic type which accepts any serializable type of value.
Following is an example that shows you how to convert a csv data to ohara row.

```
c0,c1,c2
v0,v1,v2
```

The above data can be converted to ohara row by following code.
```java
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Cell;
class ExampleOfRow {
    public static void main(String[] args) {
        Row row = Row.of(
                Cell.of("c0", "v0"),
                Cell.of("c1", "v1"),
                Cell.of("c2", "v2")
                );
    }
}
```

```
c0,c1,c2
v0,,v2
```

The above data can be converted to ohara row by following code.
```java
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Cell;
class ExampleOfRow {
    public static void main(String[] args) {
        Row row = Row.of(
                Cell.of("c0", "v0"),
                Cell.of("c2", "v2")
                );
    }
}
```

Don't worry about the serialization. Ohara offers default serializations for following data types.
1. string
1. boolean
1. short
1. int
1. long
1. float
1. double
1. bytes
1. serializable object
1. row (a nested row is acceptable!)

> The default serializer is located at com.island.ohara.common.data.Serializer.

When you get the rows in connector, you should follow the **cell setting** to generate the output.
The **cell setting** in ohara is called **column**. It shows the metadata of a **cell**. The metadata consists of
1. origin column name (**string**) — you can match the cell by this name
1. new column name — the new name of output.
1. type (**DataType**) — the type of output value. Whatever the origin type of value, you should convert the value according this type.
                        Don't worry the casting error. It is up to the user who pass the wrong configuration.
    - string
    - boolean
    - short
    - int
    - long
    - float
    - double
    - bytes
    - serializable object
    - row
1. order (**int**) — the order of cells in output.

An example of converting data according to columns.
```java
import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
class ExampleOfConverting {
    public static Object hello(Column column, String rawValue) {
        switch (column.dataType) {
            case DataType.BOOLEAN:
                return Boolean.valueOf(rawValue);
            case DataType.STRING:
                return rawValue;
            case DataType.SHORT:
                return Short.valueOf(rawValue);
            case DataType.INT:
                return Integer.valueOf(rawValue);
            case DataType.FLOAT:
                return Float.valueOf(rawValue);
            case DataType.DOUBLE:
                return Double.valueOf(rawValue);
            default:
                throw new IllegalArgumentException("unsupported type:" + column.dataType);
        }
    }
}
```

The type is a complicated issue since there are countless types in this world. It is impossible to define a general solution
to handle all types so the final types of value is **byte array** or **serializable object**. If the type you want to pass is not
in official support, you should define it as **byte array** or **serializable object** and then process it in your connectors.

> Feel free to throw an exception when your connector encounter a unknown type. Don't swallow it and convert to a weird value, such as null or empty.
Throwing exception is better than generating corrupt data! 

### Source connector
A simple connector 

----------
### Sink connector

----------
## metrics

----------
## setting definitions