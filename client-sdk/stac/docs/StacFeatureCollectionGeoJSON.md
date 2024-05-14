# StacFeatureCollectionGeoJSON


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | 
**features** | [**List[Item]**](Item.md) |  | 
**links** | [**List[CatalogLandingPageLinksInner]**](CatalogLandingPageLinksInner.md) |  | [optional] 
**time_stamp** | **datetime** | This property indicates the time and date when the response was generated. | [optional] 
**number_matched** | **int** | The number of features of the feature type that match the selection parameters like &#x60;bbox&#x60;. | [optional] 
**number_returned** | **int** | The number of features in the feature collection.  A server may omit this information in a response, if the information about the number of features is not known or difficult to compute.  If the value is provided, the value shall be identical to the number of items in the \&quot;features\&quot; array. | [optional] 

## Example

```python
from openapi_client.models.stac_feature_collection_geo_json import StacFeatureCollectionGeoJSON

# TODO update the JSON string below
json = "{}"
# create an instance of StacFeatureCollectionGeoJSON from a JSON string
stac_feature_collection_geo_json_instance = StacFeatureCollectionGeoJSON.from_json(json)
# print the JSON string representation of the object
print(StacFeatureCollectionGeoJSON.to_json())

# convert the object into a dict
stac_feature_collection_geo_json_dict = stac_feature_collection_geo_json_instance.to_dict()
# create an instance of StacFeatureCollectionGeoJSON from a dict
stac_feature_collection_geo_json_from_dict = StacFeatureCollectionGeoJSON.from_dict(stac_feature_collection_geo_json_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


