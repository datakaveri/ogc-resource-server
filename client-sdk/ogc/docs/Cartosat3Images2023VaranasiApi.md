# openapi_client.Cartosat3Images2023VaranasiApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_specific_collection**](Cartosat3Images2023VaranasiApi.md#get_specific_collection) | **GET** /collections/a718a274-3d8e-4dcf-860c-c6a4cf31e947 | Metadata about Cartosat-3 images for the year 2023 for Varanasi district


# **get_specific_collection**
> Collection get_specific_collection()

Metadata about Cartosat-3 images for the year 2023 for Varanasi district

### Example


```python
import openapi_client
from openapi_client.models.collection import Collection
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
    api_instance = openapi_client.Cartosat3Images2023VaranasiApi(api_client)

    try:
        # Metadata about Cartosat-3 images for the year 2023 for Varanasi district
        api_response = api_instance.get_specific_collection()
        print("The response of Cartosat3Images2023VaranasiApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling Cartosat3Images2023VaranasiApi->get_specific_collection: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**Collection**](Collection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Information about the feature collection with id &#x60;collectionId&#x60;. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

