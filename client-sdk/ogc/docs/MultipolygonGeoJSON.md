# MultipolygonGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[List[List[float]]]]** |  | 

## Example

```python
from openapi_client.models.multipolygon_geo_json import MultipolygonGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of MultipolygonGeoJSON from a JSON string
multipolygon_geo_json_instance = MultipolygonGeoJSON.from_json(json)
# print the JSON string representation of the object
print(MultipolygonGeoJSON.to_json())

# convert the object into a dict
multipolygon_geo_json_dict = multipolygon_geo_json_instance.to_dict()
# create an instance of MultipolygonGeoJSON from a dict
multipolygon_geo_json_from_dict = MultipolygonGeoJSON.from_dict(multipolygon_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


