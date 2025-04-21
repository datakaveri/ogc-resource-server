<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Setup and Installation Guide
This document contains the installation and configuration information required to deploy the OGC Resource Server.

## Configuration
In order to connect the OGC Resource Server with AWS S3, PostgreSQL, RabbitMQ, DX Catalogue Server, DX AAA Server, DX Auditing Server etc please refer [Configurations](./Configurations.md). It contains appropriate information which shall be updated as per the deployment.

## Dependencies
In this section we explain about the dependencies and their scope. It is expected that the dependencies are met before starting the deployment of OGC Resource Server.

| Software Name                                               | Purpose                                                                                                                                       | 
|:------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| PostGIS                                                     | For storing information related to geo spatial meta data, processes, feature collections, tiles, STAC assets, coverages,  resources and users |
| AWS S3                                                      | To serve map tiles as well as STAC asset files                                                                                                |
| RabbitMQ                                                    | To publish auditing related data to auditing server via RabbitMQ exchange                                                                     |
| DX Authentication Authorization and Accounting (AAA) Server | Used to download certificate for JWT token decoding and to get user info                                                                      |
| DX Catalogue Server                                         | Used to fetch the list of resource and provider related information                                                                           |
| DX Auditing Server                                          | Used for logging and auditing the access for metering purposes                                                                                |

## Prerequisites
- Make a config file based on the template in [example-config/config-example.json](./example-config/config-example.json)
- Set up AWS S3 for serving tiles and STAC assets
- Set up PostGIS for storing information related to geo spatial data
- Set up RabbitMQ for publishing the auditing data
- Set up the database using Flyway

#### AWS S3 setup

- AWS S3 is used to serve map tiles as well as STAC asset files
- An S3 bucket can be set up by following the [AWS S3 documentation](https://docs.aws.amazon.com/AmazonS3/latest/userguide/GetStartedWithS3.html), after which the S3 bucket name, region name, access key and secret key can be added to the config.

#### PostGIS
- [PostGIS](https://postgis.net/documentation/getting_started/) is an extension for PostgreSQL that adds support for geographic objects, allowing users to store and query spatial data.
- To setup PostgreSQL refer setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)

| Table Name               | Purpose                                                                                                                             | 
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| collections_details      | To store metadata about collections, including title, description, and bounding box                                                 |
| processes_table          | To store details of processes including input, output, and execution modes                                                          | 
| ri_details               | To store information related to resource instance (RI) details, including role and access type                                      | 
| roles                    | To store user roles, such as provider, consumer, or delegate                                                                        |
| jobs_table               | To store job details, including status, type, progress, and timestamps, related to different processes                              |
| collection_type          | To store types associated with collections, based on the type column from collections_details                                       |                                                                                               
| tilematrixset_metadata   | To store metadata for tile matrix sets, including scale, cell size, and matrix dimensions                                           |
| tilematrixsets_relation  | To store the relation between collections and tile matrix sets                                                                      |
| collection_supported_crs | To store the Coordinate Reference Systems (CRS) supported by specific collections                                                   |
| crs_to_srid	            | To map Coordinate Reference Systems (CRS) to Spatial Reference Identifiers (SRID)                                                   |
| stac_collections_assets  | To store assets linked to collections, such as thumbnails, data, and metadata, including their size, type, and role                 |
| collection_coverage	    | To store the coverage schema and associated hrefs related to collections, helping define the spatial/temporal extent of collections |

#### Auditing
- Auditing is done using the DX Auditing Server which uses Postgres for storing the audit logs for OGC Resource Server
- The schema for auditing table in PostgreSQL is present here - [postgres auditing table schema](https://github.com/datakaveri/ogc-resource-server/blob/main/src/main/resources/auditing-db/migration/V1__Initialise_audit_tables.sql)

| Table Name          | Purpose                                                        | DB           | 
|---------------------|----------------------------------------------------------------|--------------|
| auditing_ogc        | To store audit logs for operations in the OGC Resource Server  | PostgreSQL   |

#### RabbitMQ
- RabbitMQ is used to push the logs which is consumed by the auditing server
- To setup RabbitMQ refer the setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)
- After deployment of RabbitMQ, we need to ensure that there are certain prerequisites met. Incase if it is not met, please login to RabbitMQ management portal using the RabbitMQ management UI and create a the following

##### Create vHost

| Type  | Name          | Details                    |   
|-------|---------------|----------------------------|
| vHost | IUDX-INTERNAL | Create a vHost in RabbitMQ |


##### Create Exchange

| Exchange Name | Type of exchange | features | Details                                                                              |   
|---------------|------------------|----------|--------------------------------------------------------------------------------------|
| auditing      | direct           | durable  | Create an exchange in vHost IUDX-INTERNAL to allow audit information to be published |  


##### Create Queue and Bind to Exchange
| Exchange Name | Queue Name | vHost   | routing key | Details                                                                                                                                   |  
|---------------|------------|---------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| auditing      | direct     | durable | #           | Create a queue in vHost IUDX-INTERNAL to allow audit information to be consumed. Ensure that the queue is binded to the auditing exchange |

#### Database Migration using Flyway
- Database flyway migrations help in updating the schema, permissions, grants, triggers etc., with the latest version
- Each flyway schema file is versioned with the format `V<majorVersion>_<minorVersion>__<description>.sql`, ex : `V1_1__init-tables.sql` to manage the database schema and handle migrations
- The migration files are located at [src/main/resources/db/migrations](https://github.com/datakaveri/ogc-resource-server/tree/main/src/main/resources/db/migration). The following pre-requisites are needed before running `flyway`:
  1. An admin user - a database user who has created schema/table privileges for the database. It can be the super user.
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


## Installation Steps
### Maven based execution
1. Install Java 11 and maven
2. Set Environment variables
```
export LOG_LEVEL=INFO
```
3. Use the maven exec plugin based starter to start the server
   `mvn clean compile exec:java@ogc-resource-server`
4. The server will be up on port **8080**. To change the port, add `httpPort:<desired_port_number>` to the config in the `ApiServerVerticle` module. See [configs/config-example.json](https://github.com/datakaveri/ogc-resource-server/blob/main/example-config/config-example.json) for an example.

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

### Docker based execution
1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `
6. The server will be up on port **8080**. To change the port, add `httpPort:<desired_port_number>` to the config in the `ApiServerVerticle` module. See [example-config/config-example.json](./example-config/config-example.json) for an example

## Client SDK

A client SDK generated using [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) is located at [client-sdk](./client-sdk). To generate a version of the SDK derived from the latest version of the OpenAPI spec at https://geoserver.dx.geospatial.org.in, download the [OpenAPI Generator JAR file](https://github.com/OpenAPITools/openapi-generator?tab=readme-ov-file#13---download-jar) and run:

```
java -jar openapi-generator-cli.jar generate -i <URL> -g python --additional-properties removeOperationIdPrefix=true,removeOperationIdPrefixDelimiter=-,removeOperationIdPrefixCount=6 -o client-sdk --global-property models,modelTests=false,apis,apiTests=false,supportingFiles=README.md:requirements.txt:setup.py:setup.cfg:openapi_client:api_client.py:api_response.py:exceptions.py:__init__.py:configuration.py:py.types:rest.py
```

where `<URL>` can be:
- `https://geoserver.dx.geospatial.org.in/api` for OGC APIs
- `https://geoserver.dx.geospatial.org.in/stac/api` for STAC APIs
- `https://geoserver.dx.geospatial.org.in/metering/api` for metering/auditing APIs

## Logging 
### Log4j 2
- For asynchronous logging, logging messages to the console in a specific format, Apache's log4j 2 is used
- For log formatting, adding appenders, adding custom logs, setting log levels, log4j2.xml could be updated : [link](https://github.com/datakaveri/ogc-resource-server/blob/main/src/main/resources/log4j2.xml)
- Please find the reference to log4j 2 : [here](https://logging.apache.org/log4j/2.x/manual/index.html)

## Testing
### Unit Testing
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report
   `mvn clean test-compile surefire:test surefire-report:report`
3. Jacoco reports are stored in `./target/`

### Integration Testing

Integration tests for the OGC Resource Server are handled using **Rest Assured**. These tests ensure the server functionality by interacting with the system through HTTP requests and responses. Follow the steps below to run the integration tests:
1. Install Prerequisites
    - Docker (Ensure you have Docker installed and running on your system)
    - Maven (Ensure Maven is installed and configured correctly on your system)

2. Update Docker Compose Configuration
    - Before running the tests, update the necessary values in the [docker-compose.test.yml](https://github.com/datakaveri/ogc-resource-server/blob/main/docker-compose.test.yml) file. Ensure correct paths are set for the configuration files, and all the required volumes and environment variables are properly configured

3. Build Docker Images
    - First, build the Docker images required for running the tests:
   ```
   sudo docker build -t s3-mock-modded -f docker/s3_mock_modded_to_443.dockerfile .
   sudo docker build -t ogc-test -f docker/test.dockerfile .
   ```
4. Run Docker Compose
   -  Start the Docker containers needed for testing:
   ```
   sudo docker compose -f docker-compose.test.yml up integ-test
   ```
5. Run Integration Tests
    - Use Maven to run the integration tests. The `mvn verify` command will execute all the tests within the project, skipping unit tests and shaded JAR creation:
    `mvn verify -DskipUnitTests=true -DskipBuildShadedJar=true -DintTestHost=localhost -DintTestPort=8443 -Pit`

6. Test Reports
   - Maven will generate reports after running the integration tests. You can check the results under the `./target/` directory

### Compliance Testing
- Compliant testing ensures that the OGC Resource Server correctly implements OGC standards, which are essential for achieving reliability and interoperability between systems
- These tests validate the server's adherence to OGC specifications, helping ensure seamless integration with other geospatial systems
- Successfully passing these tests confirms that the server is compliant with OGC standards, which provides confidence in the server’s capabilities, ensuring it meets the high standards required for modern geospatial applications
- For more details and to review the compliance test results for our server, visit: [OGC Resource Server Compliance Test Reports](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/OGC_20Feature_20Compliance_20Test_20Reports/)

### Performance Testing
- JMeter is for used performance testing, load testing of the application
- Please find the reference to JMeter : [here](https://jmeter.apache.org/usermanual/get-started.html)
- Command to generate HTML report at `target/jmeter`
```
rm -r -f target/jmeter && jmeter -n -t jmeter/<file-name>.jmx -l target/jmeter/sample-reports.csv -e -o target/jmeter/
```

### Security Testing
- For security testing, Zed Attack Proxy(ZAP) Scanning is done to discover security risks, vulnerabilities to help us address them
- A report is generated to show vulnerabilities as high risk, medium risk, low risk and false positive
- Please find the reference to ZAP : [here](https://www.zaproxy.org/getting-started/)
