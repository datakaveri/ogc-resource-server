# openapi_client.CapabilitiesApi

All URIs are relative to *https://geoserver.dx.geospatial.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_collections**](CapabilitiesApi.md#get_collections) | **GET** /collections | the feature collections in the dataset
[**get_conformance_classes**](CapabilitiesApi.md#get_conformance_classes) | **GET** /conformance | information about specifications that the API Server conforms to
[**get_landing_page**](CapabilitiesApi.md#get_landing_page) | **GET** / | Landing page


# **get_collections**
> Collections get_collections()

the feature collections in the dataset

It will fetch the list of feature collections to DX Data Consumers based on the required format with default limit and offset values supplied. This API provides a birds-eye-view for all avialable Feature Collections. The dataset is organized as one or more feature collections. This resource provides information about and access to the collections. The response contains the list of collections. For each collection, a link to the items in the collection (path `/collections/{collectionId}/items`, link relation `items`) as well as key information about the collection. This information includes- * A local identifier for the collection that is unique for the dataset. * A list of coordinate reference systems (CRS) in which geometries may be returned by the server. The first CRS is the default coordinate reference system (the default is always WGS 84 with axis order longitude/latitude).  * An optional title and description for the collection. * An optional extent that can be used to provide an indication of the spatial and temporal extent of the collection - typically derived from the data.  * An optional indicator about the type of the items in the collection (the default value, if the indicator is not provided, is 'feature').

### Example


```python
import openapi_client
from openapi_client.models.collections import Collections
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
    api_instance = openapi_client.CapabilitiesApi(api_client)

    try:
        # the feature collections in the dataset
        api_response = api_instance.get_collections()
        print("The response of CapabilitiesApi->get_collections:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CapabilitiesApi->get_collections: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**Collections**](Collections.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The feature collections shared by this API. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_conformance_classes**
> ConfClasses get_conformance_classes()

information about specifications that the API Server conforms to

A list of all conformance classes specified as per OGC standard that the server conforms to. To support \"generic\" clients that want to access multiple OGC API Features implementations - and not \"just\" a specific API / server, the server declares the conformance classes it implements and conforms to.

### Example


```python
import openapi_client
from openapi_client.models.conf_classes import ConfClasses
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
    api_instance = openapi_client.CapabilitiesApi(api_client)

    try:
        # information about specifications that the API Server conforms to
        api_response = api_instance.get_conformance_classes()
        print("The response of CapabilitiesApi->get_conformance_classes:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CapabilitiesApi->get_conformance_classes: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**ConfClasses**](ConfClasses.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The URIs of all conformance classes supported by the server. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_landing_page**
> GetLandingPage200Response get_landing_page()

Landing page

Landing page for the OGC API GDI Resource Server. It provides links to the API definition (link relations service-desc and service-doc), the Conformance declaration (path /conformance, link relation conformance), and the Feature Collections (path /collections, link relation data). 

### Example


```python
import openapi_client
from openapi_client.models.get_landing_page200_response import GetLandingPage200Response
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
    api_instance = openapi_client.CapabilitiesApi(api_client)

    try:
        # Landing page
        api_response = api_instance.get_landing_page()
        print("The response of CapabilitiesApi->get_landing_page:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CapabilitiesApi->get_landing_page: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetLandingPage200Response**](GetLandingPage200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful query |  -  |
**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

