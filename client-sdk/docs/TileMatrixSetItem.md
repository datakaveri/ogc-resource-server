# TileMatrixSetItem

A minimal tile matrix set element for use within a list of tile matrix sets linking to a full definition.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** | Optional local tile matrix set identifier, e.g. for use as unspecified &#x60;{tileMatrixSetId}&#x60; parameter. Implementation of &#39;identifier&#39; | [optional] 
**title** | **str** | Title of this tile matrix set, normally used for display to a human | [optional] 
**uri** | **str** | Reference to an official source for this tileMatrixSet | [optional] 
**crs** | [**TileMatrixSetItemCrs**](TileMatrixSetItemCrs.md) |  | [optional] 
**links** | [**List[Link]**](Link.md) | Links to related resources. A &#39;self&#39; link to the tile matrix set definition is required. | 

## Example

```python
from openapi_client.models.tile_matrix_set_item import TileMatrixSetItem

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixSetItem from a JSON string
tile_matrix_set_item_instance = TileMatrixSetItem.from_json(json)
# print the JSON string representation of the object
print(TileMatrixSetItem.to_json())

# convert the object into a dict
tile_matrix_set_item_dict = tile_matrix_set_item_instance.to_dict()
# create an instance of TileMatrixSetItem from a dict
tile_matrix_set_item_from_dict = TileMatrixSetItem.from_dict(tile_matrix_set_item_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


