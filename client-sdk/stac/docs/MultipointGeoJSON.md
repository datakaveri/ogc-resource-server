# MultipointGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[float]]** |  | 

## Example

```python
from openapi_client.models.multipoint_geo_json import MultipointGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of MultipointGeoJSON from a JSON string
multipoint_geo_json_instance = MultipointGeoJSON.from_json(json)
# print the JSON string representation of the object
print(MultipointGeoJSON.to_json())

# convert the object into a dict
multipoint_geo_json_dict = multipoint_geo_json_instance.to_dict()
# create an instance of MultipointGeoJSON from a dict
multipoint_geo_json_from_dict = MultipointGeoJSON.from_dict(multipoint_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


