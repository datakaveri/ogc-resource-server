# PointGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**coordinates** | **List[float]** |  | 

## Example

```python
from openapi_client.models.point_geo_json import PointGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of PointGeoJSON from a JSON string
point_geo_json_instance = PointGeoJSON.from_json(json)
# print the JSON string representation of the object
print(PointGeoJSON.to_json())

# convert the object into a dict
point_geo_json_dict = point_geo_json_instance.to_dict()
# create an instance of PointGeoJSON from a dict
point_geo_json_from_dict = PointGeoJSON.from_dict(point_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


