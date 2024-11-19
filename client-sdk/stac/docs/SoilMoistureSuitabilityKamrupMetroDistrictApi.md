# openapi_client.SoilMoistureSuitabilityKamrupMetroDistrictApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_specific_collection**](SoilMoistureSuitabilityKamrupMetroDistrictApi.md#get_specific_collection) | **GET** /stac/collections/8d4c7eb7-8a88-4861-b962-d5bf342b99a1 | Metadata about Mapping areas indicating Rice-fallow areas with soil moisture suitability based on moisture content


# **get_specific_collection**
> StacCollection get_specific_collection()

Metadata about Mapping areas indicating Rice-fallow areas with soil moisture suitability based on moisture content

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
    api_instance = openapi_client.SoilMoistureSuitabilityKamrupMetroDistrictApi(api_client)

    try:
        # Metadata about Mapping areas indicating Rice-fallow areas with soil moisture suitability based on moisture content
        api_response = api_instance.get_specific_collection()
        print("The response of SoilMoistureSuitabilityKamrupMetroDistrictApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SoilMoistureSuitabilityKamrupMetroDistrictApi->get_specific_collection: %s\n" % e)
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

