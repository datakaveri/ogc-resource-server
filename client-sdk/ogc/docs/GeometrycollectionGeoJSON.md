# GeometrycollectionGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**geometries** | [**List[GeometryGeoJSON]**](GeometryGeoJSON.md) |  | 

## Example

```python
from openapi_client.models.geometrycollection_geo_json import GeometrycollectionGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of GeometrycollectionGeoJSON from a JSON string
geometrycollection_geo_json_instance = GeometrycollectionGeoJSON.from_json(json)
# print the JSON string representation of the object
print(GeometrycollectionGeoJSON.to_json())

# convert the object into a dict
geometrycollection_geo_json_dict = geometrycollection_geo_json_instance.to_dict()
# create an instance of GeometrycollectionGeoJSON from a dict
geometrycollection_geo_json_from_dict = GeometrycollectionGeoJSON.from_dict(geometrycollection_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


