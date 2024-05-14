# StacCollections


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**links** | [**List[CatalogLandingPageLinksInner]**](CatalogLandingPageLinksInner.md) |  | 
**collections** | [**List[StacCollection]**](StacCollection.md) |  | 

## Example

```python
from openapi_client.models.stac_collections import StacCollections

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollections from a JSON string
stac_collections_instance = StacCollections.from_json(json)
# print the JSON string representation of the object
print(StacCollections.to_json())

# convert the object into a dict
stac_collections_dict = stac_collections_instance.to_dict()
# create an instance of StacCollections from a dict
stac_collections_from_dict = StacCollections.from_dict(stac_collections_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


