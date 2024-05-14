# GeometryGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[List[List[float]]]]** |  | 
**geometries** | [**List[GeometryGeoJSON]**](GeometryGeoJSON.md) |  | 

## Example

```python
from openapi_client.models.geometry_geo_json import GeometryGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of GeometryGeoJSON from a JSON string
geometry_geo_json_instance = GeometryGeoJSON.from_json(json)
# print the JSON string representation of the object
print(GeometryGeoJSON.to_json())

# convert the object into a dict
geometry_geo_json_dict = geometry_geo_json_instance.to_dict()
# create an instance of GeometryGeoJSON from a dict
geometry_geo_json_from_dict = GeometryGeoJSON.from_dict(geometry_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


