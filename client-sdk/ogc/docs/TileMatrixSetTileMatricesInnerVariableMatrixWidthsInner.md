# TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner

Variable Matrix Width data structure

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**coalesce** | **float** | Number of tiles in width that coalesce in a single tile for these rows | 
**min_tile_row** | **float** | First tile row where the coalescence factor applies for this tilematrix | 
**max_tile_row** | **float** | Last tile row where the coalescence factor applies for this tilematrix | 

## Example

```python
from openapi_client.models.tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner import TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner from a JSON string
tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner_instance = TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner.from_json(json)
# print the JSON string representation of the object
print(TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner.to_json())

# convert the object into a dict
tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner_dict = tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner_instance.to_dict()
# create an instance of TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner from a dict
tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner_from_dict = TileMatrixSetTileMatricesInnerVariableMatrixWidthsInner.from_dict(tile_matrix_set_tile_matrices_inner_variable_matrix_widths_inner_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


