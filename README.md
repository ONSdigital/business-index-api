
# business-index-api
[![Build Status](https://travis-ci.org/ONSdigital/business-index-api.svg?branch=develop)](https://travis-ci.org/ONSdigital/business-index-api) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/75fd2f255d07447a9cd73fb9eb8381f1)](https://www.codacy.com/app/ONSDigital/business-index-api?utm_source=github.com&utm_medium=referral&utm_content=ONSdigital/business-index-api&utm_campaign=badger) [![Coverage Status](https://coveralls.io/repos/github/ONSdigital/business-index-api/badge.svg?branch=develop)](https://coveralls.io/github/ONSdigital/business-index-api?branch=develop) [![Dependency Status](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc)

### Prerequisites

* Java 8 or higher
* SBT (http://www.scala-sbt.org/)

### Development Setup (MacOS)

To install/run ElasticSearch on MacOS, use Homebrew (http://brew.sh):

- `brew install homebrew/versions/elasticsearch24`
- `elasticsearch`

The last command runs an interactive Elasticsearch 2.4.1 session that the application can connect to using cluster name
`elasticsearch_<your username>`. 

### Running

To compile, build and run the application (by default it will connect to your local ElasticSearch):

```shell
sbt api/run -Denvironment=local
```

To package the project in a runnable fat-jar:

```shell
sbt assembly
```

### API Documentation: swagger-ui

Swagger UI is integrated into business-api. Exposed API documented and available within url:
 
 ``` http://localhost:9000/assets/lib/swagger-ui/index.html?/url=http://localhost:9000/swagger.json ```

short path:
 ``` http://localhost:9000/docs ```

### Response Time

Each request-response interaction carries a `X-Response-Time` header with a millisecond value indicating the server
compute time.

### Dependencies

A graph detailing all project dependencies can be found [here](dependencies.txt). TODO: update
If any sbt changes performed - please re-generate dependency graph by executing:
```shell
sbt -no-colors dependencyTree > dependencies.txt
```

#### HBase (experimental)

Introduced requests caching in HBase. By default caching is disabled (configurable in application.conf).
HBase can be installed locally with
```shell
brew install hbase
```
and started as:
```shell
start-hbase.sh
```

There should be *es_requests* table be created with 'd' column family.
Open hbase shell and execute:

```shell
hbase shell
create 'es_requests', 'd'
```

Other useful shell commands:

```shell
list
scan 'es_requests'
count 'es_requests'
truncate 'es_requests'
```

#### Configuring Splunk Logging

Edit [`conf/logback.xml`](conf/logback.xml) and edit the `SPLUNKSOCKET` appender configuration. By default, 
the configuration assumes that you have Splunk running on your local machine (`127.0.0.1`) with a TCP input configured
on port `15000`. Note that TCP inputs are *not* the same as Splunk's management port.

You can control the format of what is logged by changing the encoder 
(see http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout for details), but the default pattern produces 
a simple timestamp, followed by the full message and a newline, like the following:

```
2016-10-26 14:54:38,461 [%thread] %level text of my event
```
