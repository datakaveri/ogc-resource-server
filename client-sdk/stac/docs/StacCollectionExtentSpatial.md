# StacCollectionExtentSpatial

The spatial extent of the features in the collection.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**bbox** | **List[List[float]]** | One or more bounding boxes that describe the spatial extent of the dataset.  The first bounding box describes the overall spatial extent of the data. All subsequent bounding boxes describe  more precise bounding boxes, e.g., to identify clusters of data. Clients only interested in the overall spatial extent will only need to access the first item in each array. | 
**crs** | **str** | Coordinate reference system of the coordinates in the spatial extent (property &#x60;bbox&#x60;). The default reference system is WGS 84 longitude/latitude. In the Core this is the only supported coordinate reference system. Extensions may support additional coordinate reference systems and add additional enum values. | [optional] [default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84']

## Example

```python
from openapi_client.models.stac_collection_extent_spatial import StacCollectionExtentSpatial

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollectionExtentSpatial from a JSON string
stac_collection_extent_spatial_instance = StacCollectionExtentSpatial.from_json(json)
# print the JSON string representation of the object
print(StacCollectionExtentSpatial.to_json())

# convert the object into a dict
stac_collection_extent_spatial_dict = stac_collection_extent_spatial_instance.to_dict()
# create an instance of StacCollectionExtentSpatial from a dict
stac_collection_extent_spatial_from_dict = StacCollectionExtentSpatial.from_dict(stac_collection_extent_spatial_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


