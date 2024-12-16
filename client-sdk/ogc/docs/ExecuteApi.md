# openapi_client.ExecuteApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**execute**](ExecuteApi.md#execute) | **POST** /processes/{processId}/execution | execute a process.


# **execute**
> StatusInfo execute(process_id, execute)

execute a process.

Create a new job by initiating the execution of a specified process identified by the 'processId'.

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.execute import Execute
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
    api_instance = openapi_client.ExecuteApi(api_client)
    process_id = 'process_id_example' # str | 
    execute = {"inputs":{"fileName":"FileName.gpkg","description":"Description of the file","title":"Title of the file","resourceId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","version":"1.0.0"},"response":"raw"} # Execute | Mandatory JSON payload for the execute request.

    try:
        # execute a process.
        api_response = api_instance.execute(process_id, execute)
        print("The response of ExecuteApi->execute:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ExecuteApi->execute: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **process_id** | **str**|  | 
 **execute** | [**Execute**](Execute.md)| Mandatory JSON payload for the execute request. | 

### Return type

[**StatusInfo**](StatusInfo.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Started asynchronous execution. Created job. |  * Location - URL to check the status of the execution/job. <br>  |
**404** | The requested URI was not found. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

