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
| catRequestItemsUri                |     String     | /iudx/cat/v1/item     | Catalogue **item** API path — used by `CatalogueService.getCatalogueAsset` for every authZ/onboarding metadata lookup |
| auditingExchange                  |     String     | auditing              | RabbitMQ exchange the audit log is published to                                                  |
| auditingRoutingKey                |     String     | ##                    | Routing key used with `auditingExchange`                                                         |
| issuer                            |     String     | dx.example.org/controlplane | Default token issuer (see the API-server `issuers` block for the live validation config)    |
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
| isWorkerVerticle    |    Boolean     | false                              | Specifies if this is a worker verticle (for blocking operations) |
| verticleInstances   |    Integer     | 2                                  | Number of verticle instances to be deployed                      |
| httpPort            |    Integer     | 8081                               | Port for running the instance of OGC Resource Server             |
| ssl                 |    Boolean     | true                               | Indicates whether SSL is enabled                                 |
| keystore            |     String     | keystore.jks                       | Path to the SSL keystore file                                    |
| keystorePassword    |     String     | password                           | Password for the keystore file                                   |
| authServerHost      |     String     | authvertx.iudx.io                  | Host name of DX AAA Server                                       |
| issuer              |     String     | issuer                             | Issuer for JWT token validation                                  |
| audience            |     String     | audience                           | Audience for JWT token validation                                |
| s3BucketName        |     String     | aws-bucket-name                    | Name of the AWS S3 bucket used for file storage                  |
| s3Region            |     String     | aws-region                         | AWS region for the S3 bucket                                     |
| s3AccessKey         |     String     | accessKey                          | AWS access key for S3 authentication                             |
| s3SecretKey         |     String     | secretKey                          | AWS secret key for S3 authentication                             |

> **Auth v2 keys.** Since the multi-issuer JWKS flow replaced the single-certificate flow, the
> API-server module reads the keys below. `authServerHost` / a single `issuer`+`audience` pair
> are no longer what authenticates a request — `issuers` is.

| Key Name                | Value Datatype | Value Example                                                      | Description                                                                                       |
|:------------------------|:--------------:|:-------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------|
| issuers                 |     Object     | see below                                                          | Map of JWT issuer (`iss` claim) → `{type, jwksUrl}`. A token whose issuer is absent is rejected 401 |
| issuers.\<iss\>.type    |     String     | remote                                                             | Only `remote` is supported today (JWKS fetched over HTTP)                                          |
| issuers.\<iss\>.jwksUrl |     String     | https://dx.example.org/.../certs                                    | JWKS endpoint for that issuer; the key set is cached per issuer                                    |
| issuers.jwtLeeway       |    Integer     | 60                                                                 | Clock-skew leeway in seconds when validating expiry                                                |
| issuers.jwtIgnoreExpiry |    Boolean     | false                                                              | Skip expiry validation (non-production only)                                                       |
| controlPanelHost        |     String     | dx.example.org                                                     | Host of the ACL control plane queried by `AclClient` for non-OPEN resources                        |
| controlPanelPort        |    Integer     | 443                                                                | Port of the ACL control plane                                                                      |
| controlPanelSearchPath  |     String     | /controlplane/iudx/acl/apd/v2/access_request/has_access            | "has access" endpoint path; HTTP 200 ⇒ access granted, anything else ⇒ 403. The caller's `did` header is forwarded, so a delegated request is checked for the delegator |
| geomSpecificMaxLimits   |     Object     | {"POINT": 10000, "POLYGON": 10}                                    | Per-PostGIS-geometry cap (and default) for the `limit` query parameter, baked into the generated OpenAPI spec |
| controlplaneGrpcHost    |     String     | dx.example.org                                                     | **Required.** dx-controlplane gRPC host used to resolve `did` delegations (`ResolveDelegation`); the server does not start without it |
| controlplaneGrpcPort    |    Integer     | 9090                                                               | dx-controlplane gRPC port (default 9090)                                                           |
| keycloakTokenUrl        |     String     | https://dx.example.org/auth/realms/\<realm\>/protocol/openid-connect/token | Keycloak token endpoint for the service's `client_credentials` token sent on every gRPC call |
| grpcClientId            |     String     | svc-ogc-resource-server                                            | Keycloak service client id; must be listed in controlplane's `grpcAllowedServiceClients`           |
| grpcClientSecret        |     String     | secret                                                             | Secret of `grpcClientId`                                                                           |

> **Scopes and delegation.** After the JWT is verified, the token's `realm_access.roles` are mapped
> to scopes (consumer → `data-access`, provider → `own-asset-management`, …; same table as dx-common)
> and route authorization checks scopes only. When a request carries a `did` header, the server
> calls controlplane's `ResolveDelegation` over gRPC; on an active delegation the request runs as the
> delegator with the delegation's scopes (the caller is kept as `delegatee_sub` and audited as
> `role=delegate`), otherwise it is rejected with 403.

## Catalogue Verticle

| Key Name            | Value Datatype | Value Example                        | Description                                                  |
|:--------------------|:--------------:|:-------------------------------------|:-------------------------------------------------------------|
| id                  |     String     | ogc.rs.catalogue.CatalogueVerticle   | Identifier for the Catalogue Verticle                        |
| verticleInstances   |    Integer     | 1                                    | Number of verticle instances to be deployed                  |
| catServerHost       |     String     | dx.example.org                       | DX Catalogue host                                            |
| catServerPort       |    Integer     | 443                                  | DX Catalogue port                                            |
| dxCatalogueBasePath |     String     | /controlplane/iudx/v2/cat            | Catalogue base path (search API is derived from it)          |
| catRequestItemsUri  |     String     | /controlplane/iudx/v2/cat/item       | Catalogue item API path used for asset metadata lookups      |

## S3 Buckets Configuration (`s3BucketsConfig`)

Required at the top level — `Deployer` refuses to start without it. Each key is a **bucket
identifier** referenced by process inputs (`s3BucketIdentifier`) and by `s3_bucket_id` columns
in the database, so one deployment can serve datasets from several providers at once.

| Key Name        | Value Datatype | Value Example                          | Description                                                      |
|:----------------|:--------------:|:---------------------------------------|:------------------------------------------------------------------|
| bucket          |     String     | my-bucket                              | Bucket name                                                       |
| region          |     String     | ap-south-1                             | Region                                                            |
| endpoint        |     String     | https://s3.ap-south-1.amazonaws.com    | Endpoint (GDAL is given this without the scheme)                  |
| accessKey       |     String     | accessKey                              | Access key                                                        |
| secretKey       |     String     | secretKey                              | Secret key                                                        |
| pathBasedAccess |    Boolean     | false                                  | `true` for MinIO-style path addressing (`AWS_VIRTUAL_HOSTING=FALSE`) |
| readAccess      |     String     | PRIVATE                                | `PUBLIC` or `PRIVATE`; reported by `GET /listConfiguredS3Buckets` |

## Jobs Verticle

| Key Name          | Value Datatype | Value Example                   | Description                                  |
|:------------------|:--------------:|:--------------------------------|:---------------------------------------------|
| id                |     String     | ogc.rs.jobs.JobsVerticle        | Identifier for the API server verticle       |
| verticleInstances |    Integer     | 1                               | Number of verticle instances to be deployed  |

## Database Verticle

| Key Name                         | Value Datatype | Value Example                    | Description                                                                                     |
|:---------------------------------|:--------------:|:---------------------------------|:------------------------------------------------------------------------------------------------|
| id                               |     String     | ogc.rs.database.DatabaseVerticle | Identifier for the Database Verticle                                                            |
| isWorkerVerticle                 |    boolean     | false                            | To check if worker verticle needs to be deployed for blocking operations                        |
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
