# Collection


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | [optional] 
**item_type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**description** | **str** |  | [optional] 
**crs** | **List[str]** | a global list of CRS identifiers that are supported by spatial feature collections offered by the service | [optional] 
**links** | [**List[GetLandingPage200ResponseLinksInner]**](GetLandingPage200ResponseLinksInner.md) |  | [optional] 

## Example

```python
from openapi_client.models.collection import Collection

# TODO update the JSON string below
json = "{}"
# create an instance of Collection from a JSON string
collection_instance = Collection.from_json(json)
# print the JSON string representation of the object
print(Collection.to_json())

# convert the object into a dict
collection_dict = collection_instance.to_dict()
# create an instance of Collection from a dict
collection_from_dict = Collection.from_dict(collection_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


