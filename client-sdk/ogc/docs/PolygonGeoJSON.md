# PolygonGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[List[List[float]]]** |  | 

## Example

```python
from openapi_client.models.polygon_geo_json import PolygonGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of PolygonGeoJSON from a JSON string
polygon_geo_json_instance = PolygonGeoJSON.from_json(json)
# print the JSON string representation of the object
print(PolygonGeoJSON.to_json())

# convert the object into a dict
polygon_geo_json_dict = polygon_geo_json_instance.to_dict()
# create an instance of PolygonGeoJSON from a dict
polygon_geo_json_from_dict = PolygonGeoJSON.from_dict(polygon_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


