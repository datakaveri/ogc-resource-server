# SchemasFeatureCollectionGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**features** | [**List[FeatureGeoJSON]**](FeatureGeoJSON.md) |  | 

## Example

```python
from openapi_client.models.schemas_feature_collection_geo_json import SchemasFeatureCollectionGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of SchemasFeatureCollectionGeoJSON from a JSON string
schemas_feature_collection_geo_json_instance = SchemasFeatureCollectionGeoJSON.from_json(json)
# print the JSON string representation of the object
print(SchemasFeatureCollectionGeoJSON.to_json())

# convert the object into a dict
schemas_feature_collection_geo_json_dict = schemas_feature_collection_geo_json_instance.to_dict()
# create an instance of SchemasFeatureCollectionGeoJSON from a dict
schemas_feature_collection_geo_json_from_dict = SchemasFeatureCollectionGeoJSON.from_dict(schemas_feature_collection_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


