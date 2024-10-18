<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Explanation
This section provides an extensive explanation of the various services exposed by the OGC Resource Server, detailing their purpose, usage, and output. The OGC Resource Server adheres to key [Open Geospatial Consortium (OGC) standards](https://www.ogc.org/standards/), facilitating the management and dissemination of geospatial data through RESTful APIs. These services include endpoints for managing geospatial features, collections, tiles, processes, and spatiotemporal assets. Additionally, the server implements audit and metering services for tracking API usage and enforcing compliance with resource access policies.

Additionally, anyone can verify if the server conforms to OGC standards by visiting the following links:

- [OGC Conformance](https://geoserver.dx.ugix.org.in/conformance)
- [STAC Conformance](https://geoserver.dx.ugix.org.in/stac/conformance)

## 1. Landing Page
The Landing Page of the OGC Resource Server is the entry point to all other services provided by the server. It provides links to various API endpoints and metadata, giving users an overview of what the server offers and how to interact with it. The landing page contains references to key services like collections, processes, and tiles, ensuring that users can quickly navigate to the relevant resources. The metadata provided [here](https://ogc.iudx.io/) outlines the structure of the API, the supported formats (such as GeoJSON and OGC-compliant formats), and the conformance classes the server adheres to.

The landing page is typically accessed via the root endpoint (/), and it responds with a set of links formatted in JSON, pointing to various resources on the server. This makes it easier for developers and systems to discover available services and data sets. The links may include descriptions, types (such as application/json), and titles, making it a user-friendly starting point for anyone looking to use the server’s capabilities. 

For more information, you can refer [here](https://ogcapi.ogc.org/common/)

## 2. Feature Services
The Feature Services allow users to retrieve both individual geospatial features and all features within a specific collection. These features, representing points, lines, or polygons, are returned in GeoJSON format with detailed information about their geometry and properties. Users can retrieve a specific feature from a collection, or query for multiple features, filtering results based on parameters like bounding box (bbox), datetime, and limit.
This service is essential for applications that require access to detailed geographic entities, supporting spatial queries or visualizations over a specified region or time period. The response includes links to related resources, enabling further exploration of the geospatial data.

For more information, you can refer [here](https://ogcapi.ogc.org/features/)

## 3. Collection Services
The Collection Services provide access to metadata about the available geospatial collections on the server. A collection is a group of related features, and this service allows users to discover and explore different datasets. Users can retrieve metadata for all collections, including details such as the spatial and temporal extent, helping them understand the scope and content of the data.
Additionally, users can retrieve more detailed metadata for a specific collection, including its geographic coverage and temporal extent. This helps users evaluate the relevance of the collection before querying individual features or tiles. The response also includes links to related services, facilitating easy navigation and further exploration of geospatial data.

## 4. Tiles Services
The Tile Retrieval service is a key component of the server’s ability to support visualizations and map-based applications. A tile is a small, rectangular piece of a larger map or dataset that is retrieved individually based on its coordinates and zoom level. Tiles are especially useful for efficient rendering of large geospatial datasets, as they allow applications to load only the parts of the map that are currently in view.
To retrieve a tile, clients must specify the collection ID, the tile matrix set ID, and the tile’s row, column, and zoom level. The server supports both raster tiles (such as PNG images) and vector tiles (such as Mapbox Vector Tiles). Depending on the type of data being requested (e.g., aerial imagery or vectorized building footprints), the server responds with the appropriate tile format.
This service is optimized for high performance, as tile-based rendering is crucial for web mapping applications, mobile GIS apps, and any other platform that requires interactive geospatial visualizations. It supports requests for large-scale datasets by breaking them into small, manageable tiles, which can be requested and rendered on demand.

For more information, you can refer [here](https://ogcapi.ogc.org/tiles/)
## 5. Process Services
The Processes Service in the OGC Resource Server is highly adaptable, compliant to OGC Processes API, supporting various geospatial tasks such as feature collection onboarding, appending data to existing feature collections, tiles onboarding, and vector integration. The feature collection onboarding process allows users to add new geospatial datasets, while the appending process expands existing feature collections by adding new data seamlessly.
The tiles onboarding process handles the integration of new tile layers for map visualizations, ensuring smooth tile setup. Additionally, vector integration facilitates tasks like transforming and analyzing vector data. The service also supports the generation of S3 pre-signed URLs, which are essential for securely uploading or accessing data on Amazon S3. This flexibility makes the
Processes API a key tool for managing, updating, and interacting with geospatial collections and tiles, as well as facilitating secure cloud storage operations.

For more information, you can refer [here](https://ogcapi.ogc.org/processes/)
## 6. STAC API Services

The STAC (SpatioTemporal Asset Catalog) API allows access to spatiotemporal assets such as satellite imagery and geospatial datasets, designed to manage large collections of geospatial data. The current implementation provides access to the STAC catalog, enabling users to browse available collections of geospatial data and their associated resources.
Users can retrieve metadata for STAC collections, which provide detailed information about spatiotemporal data sets. Additionally, the API allows access to individual collections, where users can explore specific geospatial datasets. Each collection includes links to assets, which represent the actual data files or resources.
The service is useful for cataloging and exploring geospatial data in a structured and manageable way.

For more information, you can refer [here](https://stacspec.org/en)
## 7. Coverage Services
The Coverage Services in the OGC Resource Server handle geospatial coverages, representing continuous phenomena like elevation, temperature, or rainfall across spatial areas. The Retrieve Coverage Data service allows users to access coverage data, with support for formats such as GeoTIFF or NetCDF, which can be processed in GIS applications for analyzing continuous data fields.
Currently, users can only retrieve coverage data by specifying the coverage ID. This service is essential for applications working with continuous spatial data, including environmental modeling, climate analysis, or topography visualization.

For more information, you can refer [here](https://ogcapi.ogc.org/coverages/)

## 8.Audit and Metering Services

The Audit Service is responsible for tracking and logging all API calls made to the OGC Resource Server. Every successful API request is recorded in an audit log, capturing metadata such as the user ID, the resource ID accessed, the time of the request, and the response size. This service is essential for monitoring and managing access to geospatial resources, ensuring that users comply with access policies, and providing transparency in API usage.

The service processes audit logs asynchronously using RabbitMQ for queuing, making it efficient and scalable. Audit logs are especially important in environments where data access needs to be controlled or where usage-based billing is implemented. For example, an organization might track how many API calls a user makes in a given month and charge them accordingly. The audit service ensures that all API interactions are accounted for and can be reviewed later if needed.
