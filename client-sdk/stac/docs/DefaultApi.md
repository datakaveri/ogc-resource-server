# openapi_client.DefaultApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_stac_item_by_id**](DefaultApi.md#get_stac_item_by_id) | **GET** /stac/collections/{collectionId}/items/{itemId} | Your GET endpoint
[**get_stac_items**](DefaultApi.md#get_stac_items) | **GET** /stac/collections/{collectionId}/items | Your GET endpoint


# **get_stac_item_by_id**
> get_stac_item_by_id(collection_id, item_id)

Your GET endpoint

### Example


```python
import openapi_client
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
    api_instance = openapi_client.DefaultApi(api_client)
    collection_id = 'collection_id_example' # str | local identifier of a collection
    item_id = 'item_id_example' # str | local identifier of an item

    try:
        # Your GET endpoint
        api_instance.get_stac_item_by_id(collection_id, item_id)
    except Exception as e:
        print("Exception when calling DefaultApi->get_stac_item_by_id: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**| local identifier of a collection | 
 **item_id** | **str**| local identifier of an item | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Downloads the file associated with the asset identified by &#39;assetId&#39;. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_stac_items**
> get_stac_items(collection_id, limit=limit, offset=offset)

Your GET endpoint

### Example


```python
import openapi_client
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
    api_instance = openapi_client.DefaultApi(api_client)
    collection_id = 'collection_id_example' # str | 
    limit = '10' # str | integer [ 1 .. 100 ] Default: 10 The optional limit parameter recommends the number of items that should be present in the response document.  If the limit parameter value is greater than advertised limit maximum, the server must return the maximum possible number of items, rather than responding with an error.  Only items are counted that are on the first level of the collection in the response document. Nested objects contained within the explicitly requested items must not be counted. (optional) (default to '10')
    offset = 'offset_example' # str | OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum = 0. Maximum = 1000. Default = 10. (optional)

    try:
        # Your GET endpoint
        api_instance.get_stac_items(collection_id, limit=limit, offset=offset)
    except Exception as e:
        print("Exception when calling DefaultApi->get_stac_items: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**|  | 
 **limit** | **str**| integer [ 1 .. 100 ] Default: 10 The optional limit parameter recommends the number of items that should be present in the response document.  If the limit parameter value is greater than advertised limit maximum, the server must return the maximum possible number of items, rather than responding with an error.  Only items are counted that are on the first level of the collection in the response document. Nested objects contained within the explicitly requested items must not be counted. | [optional] [default to &#39;10&#39;]
 **offset** | **str**| OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum &#x3D; 0. Maximum &#x3D; 1000. Default &#x3D; 10. | [optional] 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Downloads the file associated with the asset identified by &#39;assetId&#39;. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

