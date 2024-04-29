# YTI terminology API

## Requirements
- Java 17
- Gradle 8.5
- Docker

## API documentation

http://localhost:9103/terminology-api/swagger-ui/index.html

## Running locally

#### Checkout and publish yti-spring-security and yti-common-backend libraries to local maven repository

Check correct version from the [dependencies](build.gradle)
```
# In dependant library directory
git checkout vX.X.X 
./gradlew publishToMavenLocal
```

#### Create Fuseki image

Checkout yti-fuseki project and run `build.sh` script

#### Start Fuseki and OpenSearch containers from yti-compose
```
docker-compose up -d yti-fuseki-v4 yti-datamodel-opensearch
```

#### Properties

Create file `application-local.properties` to `src/main/resources/config` directory. Copy values from [template file](src/main/resources/config/application-template.properties) and adjust as needed. 

#### Run application
```
./gradlew bootRun  --args='--spring.profiles.active=local'
```