# custom streamapp guideline

ohara streamapp is a unparalleled wrap of kafka streaming which gives a way to design your streaming flow. 

----------
## Start Your Own Ohara StreamApp

Before writing your streamapp, you should download the ohara dependencies first. Ohara includes many powerful tools
for developer but not all tools are requisite in designing streamapp. The required dependencies are shown below.
  
```groovy
implementation "com.island.ohara:ohara-streams:0.4-SNAPSHOT"
implementation "com.island.ohara:ohara-common:0.4-SNAPSHOT"
implementation "com.island.ohara:ohara-kafka:0.4-SNAPSHOT"
```

Of course, you encounter a error of downloading above dependencies if you don't add ohara's maven repository.

```groovy
repositories {
     maven {
         url "https://dl.bintray.com/oharastream/ohara"
     }
 }
```

After downloading dependencies, it would be better to glance sample code of streamapp. You can find them in following
locations.

1. ohara-streams/src/test/java/com/island/ohara/streams/SimpleApplicationForExternalEnv.java
1. ohara-streams/src/test/java/com/island/ohara/streams/SimpleApplicationForOharaEnv.java
