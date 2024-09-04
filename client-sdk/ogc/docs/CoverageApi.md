# openapi_client.CoverageApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_collection_coverage**](CoverageApi.md#get_collection_coverage) | **GET** /collections/{collectionId}/coverage | retrieve the schema of the coverage-data
[**get_coverage_schema**](CoverageApi.md#get_coverage_schema) | **GET** /collections/{collectionId}/schema | retrieve the schema of the coverage-data


# **get_collection_coverage**
> get_collection_coverage(collection_id)

retrieve the schema of the coverage-data

Gives the coeverage of the collection

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://geoserver.dx.ugix.org.in
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://geoserver.dx.ugix.org.in"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure Bearer authorization (JWT): DX-AAA-Token
configuration = openapi_client.Configuration(
    access_token = os.environ["BEARER_TOKEN"]
)

# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.CoverageApi(api_client)
    collection_id = 'collection_id_example' # str | local identifier of a collection

    try:
        # retrieve the schema of the coverage-data
        api_instance.get_collection_coverage(collection_id)
    except Exception as e:
        print("Exception when calling CoverageApi->get_collection_coverage: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**| local identifier of a collection | 

### Return type

void (empty response body)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Download the coverage file associated with the collection identified by &#39;collection_id&#39;. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_coverage_schema**
> GetCoverageSchema200Response get_coverage_schema(collection_id)

retrieve the schema of the coverage-data

Shows the schema of the coverage.

### Example


```python
import openapi_client
from openapi_client.models.get_coverage_schema200_response import GetCoverageSchema200Response
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
    api_instance = openapi_client.CoverageApi(api_client)
    collection_id = 'collection_id_example' # str | Local identifier of a collection

    try:
        # retrieve the schema of the coverage-data
        api_response = api_instance.get_coverage_schema(collection_id)
        print("The response of CoverageApi->get_coverage_schema:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CoverageApi->get_coverage_schema: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **collection_id** | **str**| Local identifier of a collection | 

### Return type

[**GetCoverageSchema200Response**](GetCoverageSchema200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful response with the schema of the coverage. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

