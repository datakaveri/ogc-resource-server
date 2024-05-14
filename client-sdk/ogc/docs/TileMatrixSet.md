# TileMatrixSet

A definition of a tile matrix set following the Tile Matrix Set standard. For tileset metadata, such a description (in `tileMatrixSet` property) is only required for offline use, as an alternative to a link with a `http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme` relation type.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**title** | **str** | Title of this tile matrix set, normally used for display to a human | [optional] 
**description** | **str** | Brief narrative description of this tile matrix set, normally available for display to a human | [optional] 
**keywords** | **List[str]** | Unordered list of one or more commonly used or formalized word(s) or phrase(s) used to describe this tile matrix set | [optional] 
**id** | **str** | Tile matrix set identifier. Implementation of &#39;identifier&#39; | [optional] 
**uri** | **str** | Reference to an official source for this tileMatrixSet | [optional] 
**ordered_axes** | **List[str]** |  | [optional] 
**crs** | [**TileMatrixSetItemCrs**](TileMatrixSetItemCrs.md) |  | 
**well_known_scale_set** | **str** | Reference to a well-known scale set | [optional] 
**tile_matrices** | [**List[TileMatrixSetTileMatricesInner]**](TileMatrixSetTileMatricesInner.md) | Describes scale levels and its tile matrices | 

## Example

```python
from openapi_client.models.tile_matrix_set import TileMatrixSet

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixSet from a JSON string
tile_matrix_set_instance = TileMatrixSet.from_json(json)
# print the JSON string representation of the object
print(TileMatrixSet.to_json())

# convert the object into a dict
tile_matrix_set_dict = tile_matrix_set_instance.to_dict()
# create an instance of TileMatrixSet from a dict
tile_matrix_set_from_dict = TileMatrixSet.from_dict(tile_matrix_set_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


