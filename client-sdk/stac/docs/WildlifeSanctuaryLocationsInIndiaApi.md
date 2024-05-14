# openapi_client.WildlifeSanctuaryLocationsInIndiaApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_specific_collection**](WildlifeSanctuaryLocationsInIndiaApi.md#get_specific_collection) | **GET** /stac/collections/799ede50-1098-4d50-a854-e70346cbd4c5 | Metadata about Location information of the Wildlife sanctuaries in India.


# **get_specific_collection**
> StacCollection get_specific_collection()

Metadata about Location information of the Wildlife sanctuaries in India.

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
    api_instance = openapi_client.WildlifeSanctuaryLocationsInIndiaApi(api_client)

    try:
        # Metadata about Location information of the Wildlife sanctuaries in India.
        api_response = api_instance.get_specific_collection()
        print("The response of WildlifeSanctuaryLocationsInIndiaApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling WildlifeSanctuaryLocationsInIndiaApi->get_specific_collection: %s\n" % e)
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

