# TileSet

A resource describing a tileset based on the OGC TileSet Metadata Standard. At least one of the 'TileMatrixSet',  or a link with 'rel' http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**title** | **str** | A title for this tileset | [optional] 
**description** | **str** | Brief narrative description of this tile set | [optional] 
**data_type** | [**TileSetItemDataType**](TileSetItemDataType.md) |  | 
**crs** | [**TileMatrixSetItemCrs**](TileMatrixSetItemCrs.md) |  | 
**tile_matrix_set_uri** | **str** | Reference to a Tile Matrix Set on an offical source for Tile Matrix Sets such as the OGC NA definition server (http://www.opengis.net/def/tms/). Required if the tile matrix set is registered on an open official source. | [optional] 
**links** | [**List[Link]**](Link.md) | Links to related resources. Possible link &#39;rel&#39; values are: &#39;http://www.opengis.net/def/rel/ogc/1.0/dataset&#39; for a URL pointing to the dataset, &#39;item&#39; for a URL template to get a tile; &#39;alternate&#39; for a URL pointing to another representation of the TileSetMetadata (e.g a TileJSON file); &#39;http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme&#39; for a definition of the TileMatrixSet; &#39;http://www.opengis.net/def/rel/ogc/1.0/geodata&#39; for pointing to a single collection (if the tileset represents a single collection) | 
**tile_matrix_set_limits** | [**List[TileMatrixLimits]**](TileMatrixLimits.md) | Limits for the TileRow and TileCol values for each TileMatrix in the tileMatrixSet. If missing, there are no limits other that the ones imposed by the TileMatrixSet. If present the TileMatrices listed are limited and the rest not available at all | [optional] 

## Example

```python
from openapi_client.models.tile_set import TileSet

# TODO update the JSON string below
json = "{}"
# create an instance of TileSet from a JSON string
tile_set_instance = TileSet.from_json(json)
# print the JSON string representation of the object
print(TileSet.to_json())

# convert the object into a dict
tile_set_dict = tile_set_instance.to_dict()
# create an instance of TileSet from a dict
tile_set_from_dict = TileSet.from_dict(tile_set_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


