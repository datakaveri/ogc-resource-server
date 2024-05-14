# LinestringGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[float]]** |  | 

## Example

```python
from openapi_client.models.linestring_geo_json import LinestringGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of LinestringGeoJSON from a JSON string
linestring_geo_json_instance = LinestringGeoJSON.from_json(json)
# print the JSON string representation of the object
print(LinestringGeoJSON.to_json())

# convert the object into a dict
linestring_geo_json_dict = linestring_geo_json_instance.to_dict()
# create an instance of LinestringGeoJSON from a dict
linestring_geo_json_from_dict = LinestringGeoJSON.from_dict(linestring_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


