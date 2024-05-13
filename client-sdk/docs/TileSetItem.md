# TileSetItem

A minimal tileset element for use within a list of tilesets linking to full description of those tilesets.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**title** | **str** | A title for this tileset | [optional] 
**data_type** | [**TileSetItemDataType**](TileSetItemDataType.md) |  | 
**crs** | [**TileMatrixSetItemCrs**](TileMatrixSetItemCrs.md) |  | 
**tile_matrix_set_uri** | **str** | Reference to a Tile Matrix Set on an offical source for Tile Matrix Sets such as the OGC NA definition server (http://www.opengis.net/def/tms/). Required if the tile matrix set is registered on an open official source. | [optional] 
**links** | [**List[Link]**](Link.md) | Links to related resources. A &#39;self&#39; link to the tileset as well as a &#39;http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme&#39; link to a definition of the TileMatrixSet are required. | 

## Example

```python
from openapi_client.models.tile_set_item import TileSetItem

# TODO update the JSON string below
json = "{}"
# create an instance of TileSetItem from a JSON string
tile_set_item_instance = TileSetItem.from_json(json)
# print the JSON string representation of the object
print(TileSetItem.to_json())

# convert the object into a dict
tile_set_item_dict = tile_set_item_instance.to_dict()
# create an instance of TileSetItem from a dict
tile_set_item_from_dict = TileSetItem.from_dict(tile_set_item_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


