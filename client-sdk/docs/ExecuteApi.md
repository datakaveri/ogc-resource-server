# openapi_client.ExecuteApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**execute**](ExecuteApi.md#execute) | **POST** /processes/{processId}/execution | execute a process.


# **execute**
> StatusInfo execute(token, process_id, execute)

execute a process.

Create a new job by initiating the execution of a specified process identified by the 'processId'.

### Example


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


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.ExecuteApi(api_client)
    token = 'token_example' # str | A <b> valid Auth token </b> to process the request.
    process_id = 'process_id_example' # str | 
    execute = {"inputs":{"fileName":"FileName.gpkg","description":"Description of the file","title":"Title of the file","resourceId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","version":"1.0.0"},"response":"raw"} # Execute | Mandatory JSON payload for the execute request.

    try:
        # execute a process.
        api_response = api_instance.execute(token, process_id, execute)
        print("The response of ExecuteApi->execute:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ExecuteApi->execute: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **token** | **str**| A &lt;b&gt; valid Auth token &lt;/b&gt; to process the request. | 
 **process_id** | **str**|  | 
 **execute** | [**Execute**](Execute.md)| Mandatory JSON payload for the execute request. | 

### Return type

[**StatusInfo**](StatusInfo.md)

### Authorization

No authorization required

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

