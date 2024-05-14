# openapi_client.FeaturesApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_asset**](FeaturesApi.md#get_asset) | **GET** /assets/{assetId} | gets the assets links that can be used to download assets
[**get_conformance_declaration**](FeaturesApi.md#get_conformance_declaration) | **GET** /stac/conformance | information about specifications that this API conforms to
[**get_stac_collections**](FeaturesApi.md#get_stac_collections) | **GET** /stac/collections | the feature collections in the dataset
[**get_stac_landing_page**](FeaturesApi.md#get_stac_landing_page) | **GET** /stac | Landing Page


# **get_asset**
> get_asset(asset_id, token)

gets the assets links that can be used to download assets

Based on the 'assetId', it gives the link to asset that can be downloaded

### Example


```python
import openapi_client
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
    api_instance = openapi_client.FeaturesApi(api_client)
    asset_id = 'asset_id_example' # str | local identifier of an asset
    token = 'token_example' # str | A <b> valid Auth token </b> to process the request.

    try:
        # gets the assets links that can be used to download assets
        api_instance.get_asset(asset_id, token)
    except Exception as e:
        print("Exception when calling FeaturesApi->get_asset: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **asset_id** | **str**| local identifier of an asset | 
 **token** | **str**| A &lt;b&gt; valid Auth token &lt;/b&gt; to process the request. | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Downloads the file associated with the asset identified by &#39;assetId&#39;. |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_conformance_declaration**
> ConfClasses get_conformance_declaration()

information about specifications that this API conforms to

A list of all conformance classes specified in a standard that the server conforms to.

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
    api_instance = openapi_client.FeaturesApi(api_client)

    try:
        # information about specifications that this API conforms to
        api_response = api_instance.get_conformance_declaration()
        print("The response of FeaturesApi->get_conformance_declaration:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling FeaturesApi->get_conformance_declaration: %s\n" % e)
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

# **get_stac_collections**
> StacCollections get_stac_collections()

the feature collections in the dataset

A body of Feature Collections that belong or are used together with additional links. Request may not return the full set of metadata per Feature Collection.

### Example


```python
import openapi_client
from openapi_client.models.stac_collections import StacCollections
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
    api_instance = openapi_client.FeaturesApi(api_client)

    try:
        # the feature collections in the dataset
        api_response = api_instance.get_stac_collections()
        print("The response of FeaturesApi->get_stac_collections:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling FeaturesApi->get_stac_collections: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**StacCollections**](StacCollections.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The feature collections shared by this API. The dataset is organized as one or more feature collections. This resource provides information about and access to the collections. The response contains the list of collections. For each collection, a link to the items in the collection (path &#x60;/collections/{collectionId}/items&#x60;, link relation &#x60;items&#x60;) as well as key information about the collection. This information includes: * A local identifier for the collection that is unique for the dataset; * A list of coordinate reference systems (CRS) in which geometries may  be returned by the server. The first CRS is the default coordinate reference system (the default is always WGS 84 with axis order longitude/latitude); * An optional title and description for the collection; * An optional extent that can be used to provide an indication of the spatial and temporal extent of the collection - typically derived from the data; * An optional indicator about the type of the items in the collection (the default value, if the indicator is not provided, is &#39;feature&#39;). |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_stac_landing_page**
> CatalogLandingPage get_stac_landing_page()

Landing Page

Returns the root STAC Catalog or STAC Collection that is the entry point for users to browse with STAC Browser or for search engines to crawl. This can either return a single STAC Collection or more commonly a STAC catalog.  The landing page provides links to the API definition (link relations `service-desc` and `conformance`) and the STAC records such as collections/catalogs (link relation `child`) or items (link relation `item`).  Extensions may add additional links with new relation types.

### Example


```python
import openapi_client
from openapi_client.models.catalog_landing_page import CatalogLandingPage
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
    api_instance = openapi_client.FeaturesApi(api_client)

    try:
        # Landing Page
        api_response = api_instance.get_stac_landing_page()
        print("The response of FeaturesApi->get_stac_landing_page:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling FeaturesApi->get_stac_landing_page: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**CatalogLandingPage**](CatalogLandingPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The landing page provides links to the API definition (link relations &#x60;service-desc&#x60; and &#x60;service-doc&#x60;). |  -  |
**400** | Bad Request |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

