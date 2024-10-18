<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Frequently Asked Questions (FAQs)

1. How do I request for a new feature to be added or change in an existing feature?
- Please create an issue [here](https://github.com/datakaveri/ogc-resource-server/issues)
2. What do we do when there is any error during flyway migration?
- We could run this command `mvn flyway:repair` and do the flyway migration again
-If the error persists, it needs to be resolved manually and a backup of the database could be taken from postgres if the table needs to be changed
3. What is OGC?
- The Open Geospatial Consortium (OGC) is an international organization that establishes standards for geospatial content and services, enabling interoperability between different systems and applications. You can learn more about OGC on their [official website]((https://ogcapi.ogc.org/))
4. What is STAC?
- The SpatioTemporal Asset Catalog (STAC) is an initiative aimed at standardizing the way geospatial assets are organized, discovered, and accessed. STAC provides a specification for describing geospatial data and its metadata, making it easier to work with satellite imagery, maps, and other geospatial resources. For more details, visit the [STAC website](https://stacspec.org/en)
5. Being a provider, how do I onboard a geospatial collection ?
- You can onboard a geospatial collection through the OGC Resource Server's Processes API. Use the POST `/processes/{id}/execution` endpoint to onboard a collection with the required input parameters (e.g., resourceId, fileName). You can then track the status of the job using GET `/jobs/{jobId}`

