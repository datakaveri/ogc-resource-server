# openapi_client.TilingSchemesApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_tile_matrix_set**](TilingSchemesApi.md#get_tile_matrix_set) | **GET** /tileMatrixSets/{tileMatrixSetId} | Retrieve the definition of the specified tiling scheme (tile matrix set)
[**get_tile_matrix_sets_list**](TilingSchemesApi.md#get_tile_matrix_sets_list) | **GET** /tileMatrixSets | Retrieve the list of available tiling schemes (tile matrix sets)


# **get_tile_matrix_set**
> TileMatrixSet get_tile_matrix_set(tile_matrix_set_id)

Retrieve the definition of the specified tiling scheme (tile matrix set)

Retrieve the definition of the specified tiling scheme (tile matrix set)

### Example


```python
import openapi_client
from openapi_client.models.tile_matrix_set import TileMatrixSet
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
    api_instance = openapi_client.TilingSchemesApi(api_client)
    tile_matrix_set_id = openapi_client.TileMatrixSets() # TileMatrixSets | Identifier for a supported TileMatrixSet

    try:
        # Retrieve the definition of the specified tiling scheme (tile matrix set)
        api_response = api_instance.get_tile_matrix_set(tile_matrix_set_id)
        print("The response of TilingSchemesApi->get_tile_matrix_set:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TilingSchemesApi->get_tile_matrix_set: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tile_matrix_set_id** | [**TileMatrixSets**](.md)| Identifier for a supported TileMatrixSet | 

### Return type

[**TileMatrixSet**](TileMatrixSet.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | tile matrix set |  -  |
**404** | The requested tile matrix set id was not found |  -  |
**406** | Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server for the requested resource. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_tile_matrix_sets_list**
> GetTileMatrixSetsList200Response get_tile_matrix_sets_list()

Retrieve the list of available tiling schemes (tile matrix sets)

Retrieve the list of available tiling schemes (tile matrix sets)

### Example


```python
import openapi_client
from openapi_client.models.get_tile_matrix_sets_list200_response import GetTileMatrixSetsList200Response
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
    api_instance = openapi_client.TilingSchemesApi(api_client)

    try:
        # Retrieve the list of available tiling schemes (tile matrix sets)
        api_response = api_instance.get_tile_matrix_sets_list()
        print("The response of TilingSchemesApi->get_tile_matrix_sets_list:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TilingSchemesApi->get_tile_matrix_sets_list: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetTileMatrixSetsList200Response**](GetTileMatrixSetsList200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | List of tile matrix sets (tiling schemes). |  -  |
**406** | Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server for the requested resource. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

