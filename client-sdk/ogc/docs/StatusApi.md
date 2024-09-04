# openapi_client.StatusApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_status**](StatusApi.md#get_status) | **GET** /jobs/{jobId} | retrieve the status of a job


# **get_status**
> StatusInfo get_status(job_id)

retrieve the status of a job

Shows the status of a job.

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.status_info import StatusInfo
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
    api_instance = openapi_client.StatusApi(api_client)
    job_id = 'job_id_example' # str | local identifier of a job

    try:
        # retrieve the status of a job
        api_response = api_instance.get_status(job_id)
        print("The response of StatusApi->get_status:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling StatusApi->get_status: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**| local identifier of a job | 

### Return type

[**StatusInfo**](StatusInfo.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The status of a job. |  -  |
**404** | The requested URI was not found. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

