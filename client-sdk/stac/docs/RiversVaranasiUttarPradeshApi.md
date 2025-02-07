# openapi_client.RiversVaranasiUttarPradeshApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_specific_collection**](RiversVaranasiUttarPradeshApi.md#get_specific_collection) | **GET** /stac/collections/7401ea7a-0807-4987-be50-900ebbbd7858 | Metadata about Rivers in Varanasi district of Uttar Pradesh along with attributes
[**get_specific_collection_0**](RiversVaranasiUttarPradeshApi.md#get_specific_collection_0) | **GET** /stac/collections/74567566-ba50-4ead-829e-79afb0fb96e4 | Metadata about  Polygon features representing the north extent of settlements in Varanasi district of Uttar Pradesh


# **get_specific_collection**
> StacCollection get_specific_collection()

Metadata about Rivers in Varanasi district of Uttar Pradesh along with attributes

### Example


```python
import openapi_client
from openapi_client.models.stac_collection import StacCollection
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
    api_instance = openapi_client.RiversVaranasiUttarPradeshApi(api_client)

    try:
        # Metadata about Rivers in Varanasi district of Uttar Pradesh along with attributes
        api_response = api_instance.get_specific_collection()
        print("The response of RiversVaranasiUttarPradeshApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RiversVaranasiUttarPradeshApi->get_specific_collection: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**StacCollection**](StacCollection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Information about the feature collection with id &#x60;collectionId&#x60;. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_specific_collection_0**
> StacCollection get_specific_collection_0()

Metadata about  Polygon features representing the north extent of settlements in Varanasi district of Uttar Pradesh

### Example


```python
import openapi_client
from openapi_client.models.stac_collection import StacCollection
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
    api_instance = openapi_client.RiversVaranasiUttarPradeshApi(api_client)

    try:
        # Metadata about  Polygon features representing the north extent of settlements in Varanasi district of Uttar Pradesh
        api_response = api_instance.get_specific_collection_0()
        print("The response of RiversVaranasiUttarPradeshApi->get_specific_collection_0:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RiversVaranasiUttarPradeshApi->get_specific_collection_0: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**StacCollection**](StacCollection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Information about the feature collection with id &#x60;collectionId&#x60;. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

