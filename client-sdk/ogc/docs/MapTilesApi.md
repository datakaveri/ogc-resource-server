# openapi_client.MapTilesApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**_collection_map_get_tile**](MapTilesApi.md#_collection_map_get_tile) | **GET** /collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol} | Retrieve a map tile from the specified collection
[**_collection_map_get_tile_set**](MapTilesApi.md#_collection_map_get_tile_set) | **GET** /collections/{collectionId}/map/tiles/{tileMatrixSetId} | Retrieve a map tile set metadata for the specified collection and tiling scheme (tile matrix set)
[**_collection_map_get_tile_sets_list**](MapTilesApi.md#_collection_map_get_tile_sets_list) | **GET** /collections/{collectionId}/map/tiles | Retrieve a list of all map tilesets for specified collection.


# **_collection_map_get_tile**
> bytearray _collection_map_get_tile(tile_matrix, tile_row, tile_col, collection_id, tile_matrix_set_id, collections=collections)

Retrieve a map tile from the specified collection

Retrieve a map tile from the specified collection

### Example


```python
import openapi_client
from openapi_client.models.tile_matrix_sets import TileMatrixSets
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MapTilesApi(api_client)
    tile_matrix = '5' # str | Identifier selecting one of the scales defined in the TileMatrixSet and representing the scaleDenominator the tile. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix=5, tileRow=10 and tileCol=15.
    tile_row = 10 # int | Row index of the tile on the selected TileMatrix. It cannot exceed the MatrixWidth-1 for the selected TileMatrix. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix=5, tileRow=10 and tileCol=15.
    tile_col = 15 # int | Column index of the tile on the selected TileMatrix. It cannot exceed the MatrixHeight-1 for the selected TileMatrix. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix=5, tileRow=10 and tileCol=15.
    collection_id = 'collection_id_example' # str | Local identifier of a collection
    tile_matrix_set_id = openapi_client.TileMatrixSets() # TileMatrixSets | Identifier for a supported TileMatrixSet
    collections = ['collections_example'] # List[str] | The collections that should be included in the response. The parameter value is a comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be included. The collection will be rendered in the order specified, with the last one showing on top, unless the priority is overridden by styling rules. (optional)

    try:
        # Retrieve a map tile from the specified collection
        api_response = api_instance._collection_map_get_tile(tile_matrix, tile_row, tile_col, collection_id, tile_matrix_set_id, collections=collections)
        print("The response of MapTilesApi->_collection_map_get_tile:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MapTilesApi->_collection_map_get_tile: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tile_matrix** | **str**| Identifier selecting one of the scales defined in the TileMatrixSet and representing the scaleDenominator the tile. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix&#x3D;5, tileRow&#x3D;10 and tileCol&#x3D;15. | 
 **tile_row** | **int**| Row index of the tile on the selected TileMatrix. It cannot exceed the MatrixWidth-1 for the selected TileMatrix. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix&#x3D;5, tileRow&#x3D;10 and tileCol&#x3D;15. | 
 **tile_col** | **int**| Column index of the tile on the selected TileMatrix. It cannot exceed the MatrixHeight-1 for the selected TileMatrix. For example, Ireland is fully within the Tile at WebMercatorQuad tileMatrix&#x3D;5, tileRow&#x3D;10 and tileCol&#x3D;15. | 
 **collection_id** | **str**| Local identifier of a collection | 
 **tile_matrix_set_id** | [**TileMatrixSets**](.md)| Identifier for a supported TileMatrixSet | 
 **collections** | [**List[str]**](str.md)| The collections that should be included in the response. The parameter value is a comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be included. The collection will be rendered in the order specified, with the last one showing on top, unless the priority is overridden by styling rules. | [optional] 

### Return type

**bytearray**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: image/png, application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | A map tile image returned as a response. |  -  |
**204** | No data available for this tile. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**406** | Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server for the requested resource. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **_collection_map_get_tile_set**
> TileSet _collection_map_get_tile_set(collection_id, tile_matrix_set_id, collections=collections)

Retrieve a map tile set metadata for the specified collection and tiling scheme (tile matrix set)

Retrieve a map tile set metadata for the specified collection and tiling scheme (tile matrix set)

### Example


```python
import openapi_client
from openapi_client.models.tile_matrix_sets import TileMatrixSets
from openapi_client.models.tile_set import TileSet
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MapTilesApi(api_client)
    collection_id = 'collection_id_example' # str | Local identifier of a collection
    tile_matrix_set_id = openapi_client.TileMatrixSets() # TileMatrixSets | Identifier for a supported TileMatrixSet
    collections = ['collections_example'] # List[str] | The collections that should be included in the response. The parameter value is a comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be included. The collection will be rendered in the order specified, with the last one showing on top, unless the priority is overridden by styling rules. (optional)

    try:
        # Retrieve a map tile set metadata for the specified collection and tiling scheme (tile matrix set)
        api_response = api_instance._collection_map_get_tile_set(collection_id, tile_matrix_set_id, collections=collections)
        print("The response of MapTilesApi->_collection_map_get_tile_set:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MapTilesApi->_collection_map_get_tile_set: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**| Local identifier of a collection | 
 **tile_matrix_set_id** | [**TileMatrixSets**](.md)| Identifier for a supported TileMatrixSet | 
 **collections** | [**List[str]**](str.md)| The collections that should be included in the response. The parameter value is a comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be included. The collection will be rendered in the order specified, with the last one showing on top, unless the priority is overridden by styling rules. | [optional] 

### Return type

[**TileSet**](TileSet.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Description of the tileset |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**406** | Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server for the requested resource. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **_collection_map_get_tile_sets_list**
> CollectionMapGetTileSetsList200Response _collection_map_get_tile_sets_list(collection_id)

Retrieve a list of all map tilesets for specified collection.

Retrieve a list of all map tilesets for specified collection.

### Example


```python
import openapi_client
from openapi_client.models.collection_map_get_tile_sets_list200_response import CollectionMapGetTileSetsList200Response
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MapTilesApi(api_client)
    collection_id = 'collection_id_example' # str | Local identifier of a collection

    try:
        # Retrieve a list of all map tilesets for specified collection.
        api_response = api_instance._collection_map_get_tile_sets_list(collection_id)
        print("The response of MapTilesApi->_collection_map_get_tile_sets_list:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MapTilesApi->_collection_map_get_tile_sets_list: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**| Local identifier of a collection | 

### Return type

[**CollectionMapGetTileSetsList200Response**](CollectionMapGetTileSetsList200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | List of available tilesets. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**406** | Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server for the requested resource. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

