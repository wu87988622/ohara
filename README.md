## Ohara

a powerful ETL tool

### Getting Started

[TODO]

### Prerequisites

* JDK 1.8+
* Scala 2.11+
* gradle 4.5+

### Installing

[TODO]

### Running all backend-services by docker
(3 brokers, 3 workers, 1 mysql, 1 ftp server and 1 configurator)
```
docker run -ti --rm -p 12345:12345 islandsystems/ohara:backend start-service.sh backend --port 12345 --ttl 123
```
* port: used by Configurator (default is random)
* TTL: time to terminate backend-service (default is 365 days)

### Running configurator by docker
```
docker run -ti --rm -p 12345:12345 islandsystems/ohara:0.1-SNAPSHOT start-service.sh configurator --port 12345
```
* port: used by Configurator (default is random)

### Running manager by docker
```
docker run -ti --rm -p 5050:5050 -e "CONFIGURATOR_API=http://localhost:9999/v0" islandsystems/ohara:0.1-SNAPSHOT start-service.sh manager
```

### Running all tests

```
gradle clean test
```

### Building project without manager
```
gradle clean build -PskipManager
```

### Built With

* [Kafka](https://github.com/apache/kafka) - streaming tool
* [AKKA](https://akka.io/) - message-driven tool
* [Gradle](https://gradle.org) - dependency Management
* [SLF4J](https://www.slf4j.org/) - LOG wrapper
* [SCALALOGGING](https://github.com/typesafehub/scalalogging) - LOG wrapper
* [LOG4J](https://logging.apache.org/log4j/2.x/) - log plugin default

### Versioning

[TODO]

### Authors

* **Vito Jeng (vito@is-land.com.tw)** - leader
* **Yung-An He (stana@is-land.com.tw)** - manager
* **Jimin Hsieh (jimin.hsieh@is-land.com.tw)** - committer
* **Jack Yang (jack@is-land.com.tw)** - committer
* **Chia-Ping Tsai (chia7712@is-land.com.tw)** - committer
* **Joshua_Lin (joshua@is-land.com.tw)** - committer
* **Geordie Mai (geordie@is-land.com.tw)** - committer

### License

[TODO] This project is licensed under the is-land License

