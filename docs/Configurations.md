<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Modules
This document contains the information of the configurations to setup various services and dependencies in order to bring up the OGC Resource Server. 
Please find the example configuration file [here](https://github.com/datakaveri/ogc-resource-server/blob/main/example-config/config-example.json). While running the server, config.json file could
be added [secrets](./secrets/configs/dev.json).
## Other Configuration

| Key Name                          | Value Datatype | Value Example         | Description                                                                                      |
|:----------------------------------|:--------------:|:----------------------|:-------------------------------------------------------------------------------------------------|
| version                           |     Float      | 1.0                   | config version                                                                                   |
| zookeepers                        |     Array      | ["zookeeper"]         | zookeeper configuration to deploy clustered vert.x instance                                      |
| clusterId                         |     String     | ogc-rs-cluster        | cluster id to deploy clustered vert.x instance                                                   |

## Common Configuration

| Key Name                          | Value Datatype | Value Example         | Description                                                                                      |
|:----------------------------------|:--------------:|:----------------------|:-------------------------------------------------------------------------------------------------|
| ogcBasePath                       |     String     | /                     | Base path for the OGC services                                                                   |
| dxCatalogueBasePath               |     String     | /iudx/cat/v1          | API base path for DX Catalogue server                                                            |
| dxAuthBasePath                    |     String     | /auth/v1              | API base path for DX AAA server                                                                  |
| hostName                          |     String     | https://ogc.iudx.io   | Hostname of the OGC Resource Server which specifies the base URL for accessing the OGC resources |
| domain                            |     String     | ogc.iudx.io           | The domain URL of the OGC Resource Server, which indicates the server's address                  |
| catServerHost                     |     String     | api.cat-test.iudx.io  | Host name of DX Catalogue server for fetching the information of resources, resource groups      |
| catServerPort                     |    Integer     | 443                   | Port number to access HTTPS APIs of Catalogue Server                                             |
| catRequestItemsUri                |     String     | /iudx/cat/v1/item     | API base path for DX Catalogue server items                                                      |
| databaseHost                      |     String     | dbHost                | Postgres Database IP address                                                                     |
| databasePort                      |    integer     | 5433                  | Postgres Port number                                                                             |
| databaseUser                      |     String     | dbUser                | Postgres Database user name                                                                      |
| databaseName                      |     String     | dbName                | Postgres Database name                                                                           |
| databasePassword                  |     String     | dbPassword            | Password for Postgres DB                                                                         |
| poolSize                          |    integer     | 10                    | Pool size for postgres client                                                                    |

## Api Server Verticle

| Key Name            | Value Datatype | Value Example                      | Description                                                      |
|:--------------------|:--------------:|:-----------------------------------|:-----------------------------------------------------------------|
| id                  |     String     | ogc.rs.apiserver.ApiServerVerticle | Identifier for the API server verticle                           |
| verticleInstances   |    Integer     | 2                                  | Number of verticle instances to be deployed                      |
| httpPort            |    Integer     | 8081                               | Port for running the instance of OGC Resource Server             |
| authServerHost      |     String     | authvertx.iudx.io                  | Host name of DX AAA Server                                       |
| issuer              |     String     | issuer                             | Issuer for JWT token validation                                  |
| audience            |     String     | audience                           | Audience for JWT token validation                                |
| s3BucketName        |     String     | aws-bucket-name                    | Name of the AWS S3 bucket used for file storage                  |
| s3Region            |     String     | aws-region                         | AWS region for the S3 bucket                                     |
| s3AccessKey         |     String     | accessKey                          | AWS access key for S3 authentication                             |
| s3SecretKey         |     String     | secretKey                          | AWS secret key for S3 authentication                             |

## Jobs Verticle

| Key Name          | Value Datatype | Value Example                   | Description                                  |
|:------------------|:--------------:|:--------------------------------|:---------------------------------------------|
| id                |     String     | ogc.rs.jobs.JobsVerticle        | Identifier for the API server verticle       |
| verticleInstances |    Integer     | 1                               | Number of verticle instances to be deployed  |

## Database Verticle

| Key Name                         | Value Datatype | Value Example                    | Description                                                                                     |
|:---------------------------------|:--------------:|:---------------------------------|:------------------------------------------------------------------------------------------------|
| id                               |     String     | ogc.rs.database.DatabaseVerticle | Identifier for the Database Verticle                                                            |
| verticleInstances                |    Integer     | 1                                | Number of verticle instances to be deployed                                                     |
| databaseHost                     |     String     | dbHost                           | Postgres Database IP address                                                                    |
| databasePort                     |    integer     | 5433                             | Postgres Port number                                                                            |
| databaseUser                     |     String     | dbUser                           | Postgres Database user name                                                                     |
| databaseName                     |     String     | dbName                           | Postgres Database name                                                                          |
| databasePassword                 |     String     | dbPassword                       | Password for Postgres DB                                                                        |
| poolSize                         |    integer     | 10                               | Pool size for postgres client                                                                   |

## Process Verticle

| Key Name          | Value Datatype | Value Example                    | Description                                     |
|:------------------|:--------------:|:---------------------------------|:------------------------------------------------|
| id                |     String     | ogc.rs.processes.ProcessVerticle | Identifier for the Process Verticle             |
| verticleInstances |    Integer     | 1                                | Number of verticle instances to be deployed     |
| s3BucketUrl       |     String     | aws-bucket-name                  | Name of the AWS S3 bucket used for file storage |
| awsRegion         |     String     | aws-region                       | AWS region for the S3 bucket                    |
| awsAccessKey      |     String     | accessKey                        | AWS access key for S3 authentication            |
| awsSecretKey      |     String     | secretKey                        | AWS secret key for S3 authentication            |
| awsEndPoint       |     String     | endPoint                         | AWS S3 EndPoint                                 |

## Metering Verticle

| Key Name                                      | Value Datatype | Value Example                    | Description                                                                                            |
|:----------------------------------------------|:--------------:|:---------------------------------|:-------------------------------------------------------------------------------------------------------|
| id                                            |     String     | ogc.rs.metering.MeteringVerticle | Identifier for the Metering Verticle                                                                   |
| verticleInstances                             |    Integer     | 1                                | Number of verticle instances to be deployed                                                            |
| dataBrokerIP                                  |     String     | rmqIP                            | RMQ IP address                                                                                         |
| dataBrokerPort                                |    Integer     | 24568                            | RMQ port number                                                                                        |
| prodVhost                                     |     String     | vHostName                        | Virtual host being used to send Audit information Default                                              |
| internalVhost                                 |     String     | internalVHostName                | Internal Virtual host being used to send Audit information                                             |
| externalVhost                                 |     String     | externalVHostName                | External Virtual host being used to send Audit information                                             |
| dataBrokerUserName                            |     String     | rmqUserName                      | User name for RMQ                                                                                      |
| dataBrokerPassword                            |     String     | rmqPassword                      | Password for RMQ                                                                                       |
| dataBrokerManagementPort                      |    Integer     | 28041                            | Port on which RMQ Management plugin is running                                                         |
| connectionTimeout                             |    Integer     | 6000                             | Setting connection timeout as part of RabbitMQ config options to set up webclient                      |
| requestedHeartbeat                            |    Integer     | 60                               | Defines after what period of time the peer TCP connection should be considered unreachable by RabbitMQ |
| handshakeTimeout                              |    Integer     | 6000                             | To increase or decrease the default connection time out                                                |
| requestedChannelMax                           |    Integer     | 5                                | Tells no more that 5 (or given number) could be opened up on a connection at the same time             |
| networkRecoveryInterval                       |    Integer     | 500                              | Interval to restart the connection between rabbitmq node and clients                                   |
| automaticRecoveryEnabled                      |    Boolean     | true                             | Whether automatic recovery of the connection is enabled                                                |
| meteringDatabaseHost                          |     String     | meteringDbHost                   | Postgres Metering Database IP address                                                                  |
| meteringDatabasePort                          |    integer     | 5433                             | Postgres Metering Port number                                                                          |
| meteringDatabaseUser                          |     String     | meteringDbUser                   | Postgres Metering Database user name                                                                   |
| meteringDatabaseName                          |     String     | meteringDbName                   | Postgres Metering Database name                                                                        |
| meteringDatabasePassword                      |     String     | meteringDbPassword               | Password for Postgres Metering DB                                                                      |
| poolSize                                      |    integer     | 10                               | Pool size for postgres client                                                                          |
