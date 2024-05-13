# openapi_client.ProcessListApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_processes**](ProcessListApi.md#get_processes) | **GET** /processes | retrieve the list of available processes


# **get_processes**
> ProcessList get_processes(limit=limit)

retrieve the list of available processes

The list of processes contains a summary of each process the OGC API - Processes offers, including the link to a more detailed description of the process.

### Example


```python
import openapi_client
from openapi_client.models.process_list import ProcessList
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
    api_instance = openapi_client.ProcessListApi(api_client)
    limit = 10 # int | The optional limit parameter limits the number of items that are presented in the response document.  Only items are counted that are on the first level of the collection in the response document. Nested objects contained within the explicitly requested items shall not be counted.  Minimum = 1. Maximum = 10000. Default = 10. (optional) (default to 10)

    try:
        # retrieve the list of available processes
        api_response = api_instance.get_processes(limit=limit)
        print("The response of ProcessListApi->get_processes:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ProcessListApi->get_processes: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **int**| The optional limit parameter limits the number of items that are presented in the response document.  Only items are counted that are on the first level of the collection in the response document. Nested objects contained within the explicitly requested items shall not be counted.  Minimum &#x3D; 1. Maximum &#x3D; 10000. Default &#x3D; 10. | [optional] [default to 10]

### Return type

[**ProcessList**](ProcessList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Information about the available processes |  -  |
**404** | The requested URI was not found. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

