# Custom StreamApp Guideline

Ohara streamApp is a unparalleled wrap of [kafka streams](https://kafka.apache.org/documentation/streams) which gives
a way to design your streaming flow.
It offers a simple way to implement and define actions to process data between topics.
You only have to write your logic in [start()](#start-method) method and compile your code to a jar file.
After jar file is compiled successfully, you can **deploy** your jar file to Ohara Stream and monitor your streamApp by
 [logs](#logs) and [metrics](#metrics). 

The following sections will describe how to write a streamApp application in Ohara Stream.

----------

## Quick Links

- [Overview](#ohara-streamapp-overview)
- [StreamApp Entry](#streamapp-entry)
- [StreamApp Java API](#streamapp-java-api)
- [Setting Definitions](#setting-definitions)
- [Metrics](#metrics)
- [Logs](#logs)

----------

## Ohara StreamApp Overview

Ohara streamApp is a wrap of [kafka streams](https://kafka.apache.org/documentation/streams) and provided an entry of interface
class [StreamApp](#streamapp-entry) to define user custom streaming code. A normal streamApp application will use [Row](custom_connector.md#data-model)
as data type to interactive topics in Ohara Stream.

Before writing your streamApp, you should download the ohara dependencies first. Ohara Stream includes many powerful tools
for developer but not all tools are requisite in designing streamApp. The required dependencies are shown below.
  
```groovy
repositories {
     maven {
         url "https://dl.bintray.com/oharastream/ohara"
     }
 }
implementation "com.island.ohara:ohara-streams:0.5-SNAPSHOT"
implementation "com.island.ohara:ohara-common:0.5-SNAPSHOT"
implementation "com.island.ohara:ohara-kafka:0.5-SNAPSHOT"
```

> The [releases](https://github.com/oharastream/ohara/releases) page shows the available version of ohara

----------

## StreamApp Entry

We will automatically find your custom class which should be extended by **om.island.ohara.streams.StreamApp**.

In Ohara Stream environment, the required parameters are defined in Ohara Stream UI. You only need to initial the
```OStream``` as following :
 ```text
OStream<Row> ostream = OStream.builder().toOharaEnvStream();
```

A base implementation for a custom streamApp only need to include [start()]() method, but you could include other methods
which are described below for your convenience.

The following example is a simple streamApp application which can run in Ohara environment. Note that this example is
simply start the streamApp application without doing any transformation, i.e., the source topic won't write data to the 
target topic.
```java
public class SimpleApplicationForOharaEnv extends StreamApp {

  @Override
  public void start() {
    OStream<Row> ostream = OStream.builder().cleanStart().toOharaEnvStream();
    ostream.start();
  }
}
```

> The methods we provide here belong to Ohara StreamApp, which have many powerful and friendly features. 
Native Kafka Streams API does not have these methods.

#### init() method
After we find the user custom class, the first method will be called by StreamApp is **init()**. This is an optional method that
can be used for user to initialize some external data source connections or input parameters.

#### start() method
This method will be called after [init()](#init-method). Normally, you could only define start() method for most cases in
Ohara Stream. We encourage user to use [source connector](custom_connector.md#source-connector) for importing external
data source to Ohara Stream and use topic data as custom streamApp data source in start() method.

The only object you should remember in this method is **OStream** (a.k.a. ohara streamApp). You could use this object to
construct your application and use all the powerful APIs in StreamApp.

For example:
```text
ostream
  .map(row -> Row.of(row.cell("name"), row.cell("age")))
  .filter(row -> row.cell("name").value() != null)
  .map(row -> Row.of(Cell.of("name", row.cell("name").value().toString().toUpperCase())))
  .start();
```
The above code does the following transformations:
1. pick cell of the header: `name`, `age` from each <i>row</i>
2. filter out that if `name` is null
3. convert the cell of `name` to <b>upperCase</b>

From now on, you can use the [StreamApp Java API](#streamapp-java-api) to design your own application, happy coding!

----------

## StreamApp Java API

See the java doc in source code.

----------

## Setting Definitions

Will be implemented in the near future. Also see:
https://github.com/oharastream/ohara/issues/962

----------

## Metrics

Will be implemented in the near future. Also see:
https://github.com/oharastream/ohara/issues/962

----------

## Logs

Will be implemented in the near future. Also see:
https://github.com/oharastream/ohara/issues/962