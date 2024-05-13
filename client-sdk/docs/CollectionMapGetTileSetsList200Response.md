# CollectionMapGetTileSetsList200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**links** | [**List[Link]**](Link.md) |  | [optional] 
**tilesets** | [**List[TileSetItem]**](TileSetItem.md) |  | 

## Example

```python
from openapi_client.models.collection_map_get_tile_sets_list200_response import CollectionMapGetTileSetsList200Response

# TODO update the JSON string below
json = "{}"
# create an instance of CollectionMapGetTileSetsList200Response from a JSON string
collection_map_get_tile_sets_list200_response_instance = CollectionMapGetTileSetsList200Response.from_json(json)
# print the JSON string representation of the object
print(CollectionMapGetTileSetsList200Response.to_json())

# convert the object into a dict
collection_map_get_tile_sets_list200_response_dict = collection_map_get_tile_sets_list200_response_instance.to_dict()
# create an instance of CollectionMapGetTileSetsList200Response from a dict
collection_map_get_tile_sets_list200_response_from_dict = CollectionMapGetTileSetsList200Response.from_dict(collection_map_get_tile_sets_list200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


