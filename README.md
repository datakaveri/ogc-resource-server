[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild)
[![Jenkins Tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild)
[![Jenkins Coverage](https://img.shields.io/jenkins/coverage/jacoco?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/jacoco/)
[![Integration Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F&label=integration%20tests)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/Integration_20Test_20Report/)
[![Security Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F&label=security%20tests)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/zap/)

# OGC Resource Server

<p align="center">
<img src="./readme/images/OGCArch.png">
</p>

### Features
- Supports OGC Features and STAC APIs
- Allows authorized data access based on access policies
- Enables metered data access
- Updated with latest software and client libraries
- Enhanced Software Testing

### Prerequisites
- Make a config file based on the template in [example-config/config-example.json](./example-config/config-example.json)
- Set up the database using Flyway
- Set up AWS S3 for serving tiles and STAC assets

#### Flyway Database setup

Flyway is used to manage the database schema and handle migrations. The migration files are located at [src/main/resources/db/migrations](src/main/resources/db/migrations). The following pre-requisites are needed before running `flyway`:
1. An admin user - a database user who has create schema/table privileges for the database. It can be the super user.
2. A normal user - this is the database user that will be configured to make queries from the server 
(e.g. `CREATE USER ogc WITH PASSWORD 'randompassword';`)

[flyway.conf](flyway.conf) must be updated with the required data. 
* `flyway.url` - the database connection URL
* `flyway.user` - the username of the admin user
* `flyway.password` - the password of the admin user
* `flyway.schemas` - the name of the schema under which the tables are created
* `flyway.placeholders.ogcUser` - the username of the server user

Please refer [here](https://flywaydb.org/documentation/configuration/parameters/) for more information about Flyway config parameters.

After this, the `info` command can be run to test the config. Then, the `migrate` command can be run to set up the database. At the `/ogc-resource-server` directory, run

```
mvn flyway:info -Dflyway.configFiles=flyway.conf
mvn flyway:migrate -Dflyway.configFiles=flyway.conf
```

The database details should then be added to the server config.

#### AWS S3 setup

AWS S3 is used to serve map tiles as well as STAC asset files. An S3 bucket can be set up by following the S3 documentation, after which the S3 bucket name, region name, access key and secret key can be added to the config.

<!--- ### Docker based execution
1. Install docker and docker-compose
2. Clone this repo
3. Build the images 
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose 
   ` docker-compose up prod `
6. The server will be up on port **8080**. To change the port, add `httpPort:<desired_port_number>` to the config in the `ApiServerVerticle` module. See [example-config/config-example.json](./example-config/config-example.json) for an example. --->

### Maven based execution
1. Install Java 11 and maven
2. Set Environment variables
```
export LOG_LEVEL=INFO
```
3. Use the maven exec plugin based starter to start the server 
   `mvn clean compile exec:java@ogc-resource-server`
4. The server will be up on port **8080**. To change the port, add `httpPort:<desired_port_number>` to the config in the `ApiServerVerticle` module. See [configs/config-example.json](configs/config-example.json) for an example.

### JAR based execution
1. Install Java 11 and maven
2. Set Environment variables
```
export LOG_LEVEL=INFO
```
3. Use maven to package the application as a JAR
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `ogc-resource-server-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

## Client SDK

A client SDK generated using [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) is located at [client-sdk](./client-sdk). To generate a version of the SDK derived from the latest version of the OpenAPI spec at https://geoserver.dx.gdi.org.in, download the [OpenAPI Generator JAR file](https://github.com/OpenAPITools/openapi-generator?tab=readme-ov-file#13---download-jar) and run:

```
java -jar openapi-generator-cli.jar generate -i <URL> -g python --additional-properties removeOperationIdPrefix=true,removeOperationIdPrefixDelimiter=-,removeOperationIdPrefixCount=6 -o client-sdk --global-property models,modelTests=false,apis,apiTests=false,supportingFiles=README.md:requirements.txt:setup.py:setup.cfg:openapi_client:api_client.py:api_response.py:exceptions.py:__init__.py:configuration.py:py.types:rest.py
```

where `<URL>` can be:
- `https://geoserver.dx.gdi.org.in/api` for OGC APIs
- `https://geoserver.dx.gdi.org.in/stac/api` for STAC APIs
- `https://geoserver.dx.gdi.org.in/metering/api` for metering/auditing APIs

## License
[View License](./LICENSE)

