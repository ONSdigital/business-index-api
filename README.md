
# business-index-api
[![Build Status](https://travis-ci.org/ONSdigital/business-index-api.svg?branch=develop)](https://travis-ci.org/ONSdigital/business-index-api) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/75fd2f255d07447a9cd73fb9eb8381f1)](https://www.codacy.com/app/ONSDigital/business-index-api?utm_source=github.com&utm_medium=referral&utm_content=ONSdigital/business-index-api&utm_campaign=badger) [![Coverage Status](https://coveralls.io/repos/github/ONSdigital/business-index-api/badge.svg?branch=develop)](https://coveralls.io/github/ONSdigital/business-index-api?branch=develop) [![Dependency Status](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc)

### Prerequisites

* Java 8 or higher
* SBT (http://www.scala-sbt.org/)
* Docker (https://www.docker.com/)

### Development Setup

To run ElasticSearch, use Docker:

```shell
docker pull docker.elastic.co/elasticsearch/elasticsearch:5.6.9
docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:5.6.9
```

### Running

To run the business-index-api locally, including the loading of 100,000 test records, run the following:

```shell
sbt "run -DONS_BI_ES_RECREATE_INDEX=true -DONS_BI_ES_LOAD_TEST_DATA=true"
```

### Packaging

To package the project into a runnable fat-jar:

```shell
sbt assembly
```

To package the project into a `.zip` file ready for deployment to CloudFoundry, run the following:

```shell
sbt universal:packageBin
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

### License

Copyright © 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE.md) for details.