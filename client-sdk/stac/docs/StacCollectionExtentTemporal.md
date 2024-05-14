# StacCollectionExtentTemporal

The temporal extent of the features in the collection.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**interval** | **List[List[Optional[datetime]]]** | One or more time intervals that describe the temporal extent of the dataset. The first time interval describes the overall temporal extent of the data. All subsequent time intervals  describe more precise time intervals, e.g., to identify  clusters of data. Clients only interested in the overall extent will only need to access the first item in each array. | 
**trs** | **str** | Coordinate reference system of the coordinates in the temporal extent (property &#x60;interval&#x60;). The default reference system is the Gregorian calendar. In the Core this is the only supported temporal reference system. Extensions may support additional temporal reference systems and add additional enum values. | [optional] [default to 'http://www.opengis.net/def/uom/ISO-8601/0/Gregorian']

## Example

```python
from openapi_client.models.stac_collection_extent_temporal import StacCollectionExtentTemporal

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollectionExtentTemporal from a JSON string
stac_collection_extent_temporal_instance = StacCollectionExtentTemporal.from_json(json)
# print the JSON string representation of the object
print(StacCollectionExtentTemporal.to_json())

# convert the object into a dict
stac_collection_extent_temporal_dict = stac_collection_extent_temporal_instance.to_dict()
# create an instance of StacCollectionExtentTemporal from a dict
stac_collection_extent_temporal_from_dict = StacCollectionExtentTemporal.from_dict(stac_collection_extent_temporal_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


