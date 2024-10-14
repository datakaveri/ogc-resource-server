[![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild)
[![Jenkins Tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild)
[![Jenkins Coverage](https://img.shields.io/jenkins/coverage/jacoco?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/jacoco/)
[![Integration Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F&label=integration%20tests)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/Integration_20Test_20Report/)
[![Security Tests](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.iudx.io%2Fjob%2Fogc_resource-server_master_pipeline%2F&label=security%20tests)](https://jenkins.iudx.io/job/ogc_resource-server_master_pipeline/lastBuild/zap/)

<p align="center">
<img src="./docs/cdpg.png" width="400">
</p>

# OGC Resource Server

<p align="center">
<img src="./readme/images/OGCArch.png">
</p>

## Introduction
Data Exchange (DX) based OGC compliant resource server is designed to manage and disseminate 
geospatial data adhering to [Open Geospatial Consortium (OGC) 
standards](https://www.ogc.org/standards/), ensuring interoperability between systems. The 
server provides APIs that allow seamless integration of geospatial
data for developers, governments, and private entities. The server 
is optimized for scalability and performance, handling large 
datasets and concurrent requests. It implements robust security 
measures, including authentication and authorization.

### Features
- Supports OGC Features API to manage geospatial data such as points, lines, and polygons
- Supports STAC (SpatioTemporal Asset Catalog) API to enable efficient cataloging and retrieval of spatio-temporal assets
- Supports OGC Tiles API to serve vector and  map tiles for rendering geospatial data
- Supports OGC Processes API to facilitate the execution of geospatial processes
- Supports OGC Coverages API to manage and serve multi-dimensional data (e.g., satellite imagery, elevation models)
- Integrates with DX AAA Server for token introspection to verify and manage access before serving data to the designated user
- Integrates with DX Catalogue Server for fetching the list of resources and providers related information          
- Integrates with DX Auditing Server for logging and auditing the access for metering purposes
- Uses Vert.x and Postgres, ensuring a scalable service-mesh infrastructure
- Regularly updated with the latest software and client libraries, with enhanced testing for reliability and performance

# Explanation
## Understanding OGC Resource Server
- The section available [here](./docs/Solution_Architecture.md) explains the components/services used in implementing the OGC Resource Server

# How To Guide
## Setup and Installation
Setup and Installation guide is available [here](./docs/SETUP-and-Installation.md)

# Reference
## API Docs
API docs are available [here](https://geoserver.dx.gsx.org.in/api)

## License
[View License](./LICENSE)

