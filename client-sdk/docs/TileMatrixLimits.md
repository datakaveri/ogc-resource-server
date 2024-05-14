# TileMatrixLimits

The limits for an individual tile matrix of a TileSet's TileMatrixSet, as defined in the OGC 2D TileMatrixSet and TileSet Metadata Standard

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tile_matrix** | **str** |  | 
**min_tile_row** | **int** |  | 
**max_tile_row** | **int** |  | 
**min_tile_col** | **int** |  | 
**max_tile_col** | **int** |  | 

## Example

```python
from openapi_client.models.tile_matrix_limits import TileMatrixLimits

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixLimits from a JSON string
tile_matrix_limits_instance = TileMatrixLimits.from_json(json)
# print the JSON string representation of the object
print(TileMatrixLimits.to_json())

# convert the object into a dict
tile_matrix_limits_dict = tile_matrix_limits_instance.to_dict()
# create an instance of TileMatrixLimits from a dict
tile_matrix_limits_from_dict = TileMatrixLimits.from_dict(tile_matrix_limits_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


