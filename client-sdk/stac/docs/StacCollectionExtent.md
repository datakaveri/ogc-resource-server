# StacCollectionExtent

The extent of the features in the collection. In the Core only spatial and temporal extents are specified. Extensions may add additional  members to represent other extents, for example, thermal or pressure  ranges. The first item in the array describes the overall extent of the data.  All subsequent items describe more precise extents, e.g.,  to identify clusters of data. Clients only interested in the overall extent will only need to access the first item in each array.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**spatial** | [**StacCollectionExtentSpatial**](StacCollectionExtentSpatial.md) |  | 
**temporal** | [**StacCollectionExtentTemporal**](StacCollectionExtentTemporal.md) |  | 

## Example

```python
from openapi_client.models.stac_collection_extent import StacCollectionExtent

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollectionExtent from a JSON string
stac_collection_extent_instance = StacCollectionExtent.from_json(json)
# print the JSON string representation of the object
print(StacCollectionExtent.to_json())

# convert the object into a dict
stac_collection_extent_dict = stac_collection_extent_instance.to_dict()
# create an instance of StacCollectionExtent from a dict
stac_collection_extent_from_dict = StacCollectionExtent.from_dict(stac_collection_extent_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


