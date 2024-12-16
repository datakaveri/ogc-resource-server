# openapi_client.HistoricalDailyRainfall19012023VaranasiUPApi

All URIs are relative to *https://geoserver.dx.ugix.org.in*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_features**](HistoricalDailyRainfall19012023VaranasiUPApi.md#get_features) | **GET** /collections/52c3b306-245d-478a-9b6b-892902a19b32/items | Get features from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP
[**get_specific_collection**](HistoricalDailyRainfall19012023VaranasiUPApi.md#get_specific_collection) | **GET** /collections/52c3b306-245d-478a-9b6b-892902a19b32 | Metadata about Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP
[**get_specific_feature**](HistoricalDailyRainfall19012023VaranasiUPApi.md#get_specific_feature) | **GET** /collections/52c3b306-245d-478a-9b6b-892902a19b32/items/{featureId} | Get single feature from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP


# **get_features**
> FeatureCollectionGeoJSON get_features(bbox_crs=bbox_crs, crs=crs, bbox=bbox, datetime=datetime, limit=limit, offset=offset, drf19=drf19, district=district, drf20=drf20, drf21=drf21, year=year, drf04=drf04, drf26=drf26, drf05=drf05, drf27=drf27, drf06=drf06, drf28=drf28, drf07=drf07, drf29=drf29, drf22=drf22, drf01=drf01, drf23=drf23, drf02=drf02, drf24=drf24, drf03=drf03, drf25=drf25, mn=mn, station=station, drf08=drf08, drf09=drf09, drf30=drf30, drf31=drf31, drf10=drf10, drf15=drf15, drf16=drf16, drf17=drf17, drf18=drf18, drf11=drf11, drf12=drf12, drf13=drf13, drf14=drf14)

Get features from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.feature_collection_geo_json import FeatureCollectionGeoJSON
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
    api_instance = openapi_client.HistoricalDailyRainfall19012023VaranasiUPApi(api_client)
    bbox_crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
    crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')
    bbox = [3.4] # List[float] | Only features that have a geometry that intersects the bounding box are selected. The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (height or depth):  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2 * Maximum value, coordinate axis 3 (optional)  If the value consists of four numbers, the coordinate reference system is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate reference system is specified in the parameter `bbox-crs`.  If the value consists of six numbers, the coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in the parameter `bbox-crs`.  The query parameter `bbox-crs` is specified in OGC API - Features - Part 2: Coordinate Reference Systems by Reference.  For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).  If the vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional bounding box.  If a feature has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries. (optional)
    datetime = 'datetime_example' # str | Either a date-time or an interval. Date and time expressions adhere to RFC 3339. Intervals may be bounded or half-bounded (double-dots at start or end).  Examples:  * A date-time: \"2018-02-12T23:20:50Z\" * A bounded interval: \"2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\" * Half-bounded intervals: \"2018-02-12T00:00:00Z/..\" or \"../2018-03-18T12:31:12Z\"  Only features that have a temporal property that intersects the value of `datetime` are selected.  If a feature has multiple temporal properties, it is the decision of the server whether only a single temporal property is used to determine the extent or all relevant temporal properties. (optional)
    limit = 5000 # int |  (optional) (default to 5000)
    offset = 1 # int | OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum = 0. Maximum = 1000. Default = 10. (optional) (default to 1)
    drf19 = 'drf19_example' # str |  (optional)
    district = 'district_example' # str |  (optional)
    drf20 = 'drf20_example' # str |  (optional)
    drf21 = 'drf21_example' # str |  (optional)
    year = 'year_example' # str |  (optional)
    drf04 = 'drf04_example' # str |  (optional)
    drf26 = 'drf26_example' # str |  (optional)
    drf05 = 'drf05_example' # str |  (optional)
    drf27 = 'drf27_example' # str |  (optional)
    drf06 = 'drf06_example' # str |  (optional)
    drf28 = 'drf28_example' # str |  (optional)
    drf07 = 'drf07_example' # str |  (optional)
    drf29 = 'drf29_example' # str |  (optional)
    drf22 = 'drf22_example' # str |  (optional)
    drf01 = 'drf01_example' # str |  (optional)
    drf23 = 'drf23_example' # str |  (optional)
    drf02 = 'drf02_example' # str |  (optional)
    drf24 = 'drf24_example' # str |  (optional)
    drf03 = 'drf03_example' # str |  (optional)
    drf25 = 'drf25_example' # str |  (optional)
    mn = 'mn_example' # str |  (optional)
    station = 'station_example' # str |  (optional)
    drf08 = 'drf08_example' # str |  (optional)
    drf09 = 'drf09_example' # str |  (optional)
    drf30 = 'drf30_example' # str |  (optional)
    drf31 = 'drf31_example' # str |  (optional)
    drf10 = 'drf10_example' # str |  (optional)
    drf15 = 'drf15_example' # str |  (optional)
    drf16 = 'drf16_example' # str |  (optional)
    drf17 = 'drf17_example' # str |  (optional)
    drf18 = 'drf18_example' # str |  (optional)
    drf11 = 'drf11_example' # str |  (optional)
    drf12 = 'drf12_example' # str |  (optional)
    drf13 = 'drf13_example' # str |  (optional)
    drf14 = 'drf14_example' # str |  (optional)

    try:
        # Get features from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP
        api_response = api_instance.get_features(bbox_crs=bbox_crs, crs=crs, bbox=bbox, datetime=datetime, limit=limit, offset=offset, drf19=drf19, district=district, drf20=drf20, drf21=drf21, year=year, drf04=drf04, drf26=drf26, drf05=drf05, drf27=drf27, drf06=drf06, drf28=drf28, drf07=drf07, drf29=drf29, drf22=drf22, drf01=drf01, drf23=drf23, drf02=drf02, drf24=drf24, drf03=drf03, drf25=drf25, mn=mn, station=station, drf08=drf08, drf09=drf09, drf30=drf30, drf31=drf31, drf10=drf10, drf15=drf15, drf16=drf16, drf17=drf17, drf18=drf18, drf11=drf11, drf12=drf12, drf13=drf13, drf14=drf14)
        print("The response of HistoricalDailyRainfall19012023VaranasiUPApi->get_features:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling HistoricalDailyRainfall19012023VaranasiUPApi->get_features: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **bbox_crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]
 **crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]
 **bbox** | [**List[float]**](float.md)| Only features that have a geometry that intersects the bounding box are selected. The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (height or depth):  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2 * Maximum value, coordinate axis 3 (optional)  If the value consists of four numbers, the coordinate reference system is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate reference system is specified in the parameter &#x60;bbox-crs&#x60;.  If the value consists of six numbers, the coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in the parameter &#x60;bbox-crs&#x60;.  The query parameter &#x60;bbox-crs&#x60; is specified in OGC API - Features - Part 2: Coordinate Reference Systems by Reference.  For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).  If the vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional bounding box.  If a feature has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries. | [optional] 
 **datetime** | **str**| Either a date-time or an interval. Date and time expressions adhere to RFC 3339. Intervals may be bounded or half-bounded (double-dots at start or end).  Examples:  * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval: \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals: \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot;  Only features that have a temporal property that intersects the value of &#x60;datetime&#x60; are selected.  If a feature has multiple temporal properties, it is the decision of the server whether only a single temporal property is used to determine the extent or all relevant temporal properties. | [optional] 
 **limit** | **int**|  | [optional] [default to 5000]
 **offset** | **int**| OGC Resource server also offers way to paginate the result for queries.  If a query returns large number of records then user can use additional parameters in query parameters to limit numbers of records  to be returned.  Minimum &#x3D; 0. Maximum &#x3D; 1000. Default &#x3D; 10. | [optional] [default to 1]
 **drf19** | **str**|  | [optional] 
 **district** | **str**|  | [optional] 
 **drf20** | **str**|  | [optional] 
 **drf21** | **str**|  | [optional] 
 **year** | **str**|  | [optional] 
 **drf04** | **str**|  | [optional] 
 **drf26** | **str**|  | [optional] 
 **drf05** | **str**|  | [optional] 
 **drf27** | **str**|  | [optional] 
 **drf06** | **str**|  | [optional] 
 **drf28** | **str**|  | [optional] 
 **drf07** | **str**|  | [optional] 
 **drf29** | **str**|  | [optional] 
 **drf22** | **str**|  | [optional] 
 **drf01** | **str**|  | [optional] 
 **drf23** | **str**|  | [optional] 
 **drf02** | **str**|  | [optional] 
 **drf24** | **str**|  | [optional] 
 **drf03** | **str**|  | [optional] 
 **drf25** | **str**|  | [optional] 
 **mn** | **str**|  | [optional] 
 **station** | **str**|  | [optional] 
 **drf08** | **str**|  | [optional] 
 **drf09** | **str**|  | [optional] 
 **drf30** | **str**|  | [optional] 
 **drf31** | **str**|  | [optional] 
 **drf10** | **str**|  | [optional] 
 **drf15** | **str**|  | [optional] 
 **drf16** | **str**|  | [optional] 
 **drf17** | **str**|  | [optional] 
 **drf18** | **str**|  | [optional] 
 **drf11** | **str**|  | [optional] 
 **drf12** | **str**|  | [optional] 
 **drf13** | **str**|  | [optional] 
 **drf14** | **str**|  | [optional] 

### Return type

[**FeatureCollectionGeoJSON**](FeatureCollectionGeoJSON.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/geo+json, application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The response is a document consisting of features in the collection. The features included in the response are determined by the server based on the query parameters of the request. To support access to larger collections without overloading the client, the API supports paged access using &#x60;limit&#x60; and &#x60;offset&#x60; paramters. |  -  |
**400** | A query parameter has an invalid value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_specific_collection**
> Collection get_specific_collection()

Metadata about Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP

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
    api_instance = openapi_client.HistoricalDailyRainfall19012023VaranasiUPApi(api_client)

    try:
        # Metadata about Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP
        api_response = api_instance.get_specific_collection()
        print("The response of HistoricalDailyRainfall19012023VaranasiUPApi->get_specific_collection:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling HistoricalDailyRainfall19012023VaranasiUPApi->get_specific_collection: %s\n" % e)
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

# **get_specific_feature**
> FeatureGeoJSON get_specific_feature(feature_id, crs=crs)

Get single feature from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP

### Example

* Bearer (JWT) Authentication (DX-AAA-Token):

```python
import openapi_client
from openapi_client.models.feature_geo_json import FeatureGeoJSON
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
    api_instance = openapi_client.HistoricalDailyRainfall19012023VaranasiUPApi(api_client)
    feature_id = 56 # int | 
    crs = 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' # str |  (optional) (default to 'http://www.opengis.net/def/crs/OGC/1.3/CRS84')

    try:
        # Get single feature from Daily rainfall records 1901-2023 for seven weather stations located in Varanasi district, UP
        api_response = api_instance.get_specific_feature(feature_id, crs=crs)
        print("The response of HistoricalDailyRainfall19012023VaranasiUPApi->get_specific_feature:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling HistoricalDailyRainfall19012023VaranasiUPApi->get_specific_feature: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **feature_id** | **int**|  | 
 **crs** | **str**|  | [optional] [default to &#39;http://www.opengis.net/def/crs/OGC/1.3/CRS84&#39;]

### Return type

[**FeatureGeoJSON**](FeatureGeoJSON.md)

### Authorization

[DX-AAA-Token](../README.md#DX-AAA-Token)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/geo+json, application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | fetch the feature with id &#x60;featureId&#x60; in the feature collection with id &#x60;collectionId&#x60; |  -  |
**404** | The requested resource does not exist on the server. For example, a path parameter had an incorrect value. |  -  |
**500** | A server error occurred. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

