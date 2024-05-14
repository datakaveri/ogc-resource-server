# FeatureGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**geometry** | [**GeometryGeoJSON**](GeometryGeoJSON.md) |  | 
**properties** | **object** |  | 
**id** | [**FeatureGeoJSONId**](FeatureGeoJSONId.md) |  | [optional] 
**links** | [**List[Link]**](Link.md) |  | [optional] 

## Example

```python
from openapi_client.models.feature_geo_json import FeatureGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of FeatureGeoJSON from a JSON string
feature_geo_json_instance = FeatureGeoJSON.from_json(json)
# print the JSON string representation of the object
print(FeatureGeoJSON.to_json())

# convert the object into a dict
feature_geo_json_dict = feature_geo_json_instance.to_dict()
# create an instance of FeatureGeoJSON from a dict
feature_geo_json_from_dict = FeatureGeoJSON.from_dict(feature_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


