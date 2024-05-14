# TileMatrixSetItemCrs


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**uri** | **str** | Reference to one coordinate reference system (CRS) | 
**wkt** | **object** |  | 
**reference_system** | **object** | A reference system data structure as defined in the MD_ReferenceSystem of the ISO 19115 | 

## Example

```python
from openapi_client.models.tile_matrix_set_item_crs import TileMatrixSetItemCrs

# TODO update the JSON string below
json = "{}"
# create an instance of TileMatrixSetItemCrs from a JSON string
tile_matrix_set_item_crs_instance = TileMatrixSetItemCrs.from_json(json)
# print the JSON string representation of the object
print(TileMatrixSetItemCrs.to_json())

# convert the object into a dict
tile_matrix_set_item_crs_dict = tile_matrix_set_item_crs_instance.to_dict()
# create an instance of TileMatrixSetItemCrs from a dict
tile_matrix_set_item_crs_from_dict = TileMatrixSetItemCrs.from_dict(tile_matrix_set_item_crs_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


