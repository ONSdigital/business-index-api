# Frequently asked questions

#### How to run application locally

There are few prerequisites to run app locally.

Make sure you have installed and running `elasticsearch` server on your machine or anywhere in the network.

Make sure there is an access to: `http://<elastichost>:9200/_cat/indices`

Make sure appropriate index is created. Index name must be the same as value of property `elasticsearch.bi.name` from `application.conf`

If index does not exists, there are two possible options.
1. Create it manually based on JSON from `index.json`
PUT request must be executed with index name and json from specified file (see Create index section from elastic.co official documentation).

2. Enable index re-creation in configuration, by setting `elastic.recreate.index` to `true`. (this is default option for local envs)
Note: not for production. It will remove all data if index already exists.

Note: there is an option ``elasticsearch.local = true`` which allows to use in-memory elastic instead of separate instance.
Can be used for local testing.

Make sure there is an access to HBase. 
It can either be installed locally or configured (via `application.conf`) to remote host.
As the minimum `zookeeper.quorum` configuration has to be provided. The more complex scenario is when kerberos authorization enabled for HBase.
See README for more details of how to configure kerberos authorization with HBase.

If all above achieved, BI application can be started in one of the following ways

- Via SBT: 
```groovy
sbt "api/run -Denvironment=local"
```

- As the standalone app:
```groovy
// to build executable binary with all dependencies:
sbt assembly
// in a folder with binary:
 java -Denvironment=local -Dconfig.file=application.conf -Dhttp.port=9050 -jar ons-bi-api.jar
```

#### How to troubleshoot and fix a bug in API

Can you reproduce it?
If not - add more logging and find out more about environment where it was found initially.

Is root cause clear?
If not - divide and conquer.
1. Find out if it's API issue or input/output data problem
2. If it's API problem find specific module where issue present. 
For ex. if it's problem with query, try to perform similar query on elastic directly, instead of through API.
If it's problem with HBase - connect to it via shell and see what tables are present and what data its contains.
3. Find out if other environments works correctly. What the difference between application? indexes? Check GIT history if required.

Do tests failing? 
If not - implement new test that will cover bug related scenario and fails on it.

In case if bug related to: 
- Search in ES - add test to either `ApplicationSpec` or `IntegrationSpec`
- Feedback processing - add test to `FeedbackStore` or `FeedbackSpec`
- Event history application - add test to `EventStoreTest` or `ModifySpec`

After fixing, run all tests to make sure nothing else is broken.

#### How to understand project structure

Scala projects are executed on JVM. Every java based application has main class to run.
Play Framework provides such class:
- `play.core.server.ProdServerStart` - is used for production environment (it's registered in `build.sbt` as `mainClass`)
- `sbt run` task knows that it's Play Framework based project and start container respectively in development mode.
 
Does not matter what way application started, it will be looking for ``routes`` file to register all accessible URLs.
URLs will be mapped to `controllers` which by default must be located in package with the same name.

Some controllers may need access to external services. Those services are becoming available with *Dependency Injection*
 
Play Framework is supporting `Guice` for DI. `Module` class extends `AbstractModule` and make available all beans that might be needed by services,
including in our case:
- TypeSafe configuration module
- elastic search client
- demo data importer
etc.


