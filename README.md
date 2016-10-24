# business-index-api

### Prerequisites

* Java 8 or higher
* SBT (http://www.scala-sbt.org/)

### Building

To compile, build and run the application:

```shell
sbt run
```

To package the project in a runnable fat-jar:

```shell
sbt assembly
```

### Development Setup

To install/run on MacOS Sierra, use Homebrew (http://brew.sh/):

- `brew install elasticsearch`
- `elasticsearch`

The last command runs an interactive Elasticsearch 2.4.1 session that the application can connect to using cluster name
`elasticsearch_<your username>`. 

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