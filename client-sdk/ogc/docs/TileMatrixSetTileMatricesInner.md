# TileMatrixSetTileMatricesInner

A tile matrix, usually corresponding to a particular zoom level of a TileMatrixSet.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**title** | **str** | Title of this tile matrix, normally used for display to a human | [optional] 
**description** | **str** | Brief narrative description of this tile matrix set, normally available for display to a human | [optional] 
**keywords** | **List[str]** | Unordered list of one or more commonly used or formalized word(s) or phrase(s) used to describe this dataset | [optional] 
**id** | **str** | Identifier selecting one of the scales defined in the TileMatrixSet and representing the scaleDenominator the tile. Implementation of &#39;identifier&#39; | 
**scale_denominator** | **float** | Scale denominator of this tile matrix | 
**cell_size** | **float** | Cell size of this tile matrix | 
**corner_of_origin** | **str** | The corner of the tile matrix (_topLeft_ or _bottomLeft_) used as the origin for numbering tile rows and columns. This corner is also a corner of the (0, 0) tile. | [optional] [default to 'topLeft']
**point_of_origin** | **object** |  | 
**tile_width** | **float** | Width of each tile of this tile matrix in pixels | 
**tile_height** | **float** | Height of each tile of this tile matrix in pixels | 
**matrix_height** | **float** | Width of the matrix (number of tiles in width) | 
**matrix_width** | **float** | Height of the matrix (number of tiles in height) | 
**variable_matrix_widths** | [**List[TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner]**](TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner.md) | Describes the rows that has variable matrix width | [optional] 

## Example

```python
from openapi_client.models.tile_matrix_set_tile_matrices_inner import TileMatrixSetTileMatricesInner

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixSetTileMatricesInner from a JSON string
tile_matrix_set_tile_matrices_inner_instance = TileMatrixSetTileMatricesInner.from_json(json)
# print the JSON string representation of the object
print(TileMatrixSetTileMatricesInner.to_json())

# convert the object into a dict
tile_matrix_set_tile_matrices_inner_dict = tile_matrix_set_tile_matrices_inner_instance.to_dict()
# create an instance of TileMatrixSetTileMatricesInner from a dict
tile_matrix_set_tile_matrices_inner_from_dict = TileMatrixSetTileMatricesInner.from_dict(tile_matrix_set_tile_matrices_inner_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


