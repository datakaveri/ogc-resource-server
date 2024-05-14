# MultilinestringGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[List[float]]]** |  | 

## Example

```python
from openapi_client.models.multilinestring_geo_json import MultilinestringGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of MultilinestringGeoJSON from a JSON string
multilinestring_geo_json_instance = MultilinestringGeoJSON.from_json(json)
# print the JSON string representation of the object
print(MultilinestringGeoJSON.to_json())

# convert the object into a dict
multilinestring_geo_json_dict = multilinestring_geo_json_instance.to_dict()
# create an instance of MultilinestringGeoJSON from a dict
multilinestring_geo_json_from_dict = MultilinestringGeoJSON.from_dict(multilinestring_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


