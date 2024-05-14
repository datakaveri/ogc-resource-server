# openapi_client.MeteringApi

All URIs are relative to *https://ogc.iudx.io*

Method | HTTP request | Description
------------- | ------------- | -------------
[**consumer_audit**](MeteringApi.md#consumer_audit) | **GET** /ngsi-ld/v1/consumer/audit | consumer search
[**overview**](MeteringApi.md#overview) | **GET** /ngsi-ld/v1/overview | overview
[**provider_audit**](MeteringApi.md#provider_audit) | **GET** /ngsi-ld/v1/provider/audit | provider search
[**summary**](MeteringApi.md#summary) | **GET** /ngsi-ld/v1/summary | summary details


# **consumer_audit**
> ProviderAudit200Response consumer_audit(timerel, time, end_time, options=options, id=id, api=api, offset=offset, limit=limit)

consumer search

Consumer API can be used by a user to get detailed audit summary of all the APIs from OGC Resource Server when the user provides the required query parameters. This API could also give the total number of requests made to all the APIs from OGC Resource Server when the `option` is query parameter is count.

### Example


```python
import openapi_client
from openapi_client.models.provider_audit200_response import ProviderAudit200Response
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://ogc.iudx.io
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://ogc.iudx.io"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MeteringApi(api_client)
    timerel = 'timerel_example' # str | The temporal relation type of <b>timerel</b> to be performed.
    time = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>start time</b> for the temporal-query in `ISO8601` format. <br/> data exchange currently accepts `IST` and `UTC` time zones for the temporal query.
    end_time = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>end time</b> for the temporal-query in `ISO8601` format. <br/> data exchange currently accepts `IST` and `UTC` time zones for the temporal query.
    options = 'options_example' # str | options parameter is used for obtaining the number of hits for a query (optional)
    id = 'id_example' # str | id of the resource in catalogue (optional)
    api = 'api_example' # str | Valid DX(Data exchange) api having base path as prefix appended to api (optional)
    offset = 0 # int | This parameter specifies the <b>offset</b> for the read metering query. <br/> By default offset value is 0 and we can give offset value according our need. (optional) (default to 0)
    limit = 2000 # int | This parameter specifies the <b>limit</b> for the read metering query. <br/> By default limit value is 2000 and we can give limit value according our need. (optional) (default to 2000)

    try:
        # consumer search
        api_response = api_instance.consumer_audit(timerel, time, end_time, options=options, id=id, api=api, offset=offset, limit=limit)
        print("The response of MeteringApi->consumer_audit:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MeteringApi->consumer_audit: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **timerel** | **str**| The temporal relation type of &lt;b&gt;timerel&lt;/b&gt; to be performed. | 
 **time** | **datetime**| This parameter specifies the &lt;b&gt;start time&lt;/b&gt; for the temporal-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones for the temporal query. | 
 **end_time** | **datetime**| This parameter specifies the &lt;b&gt;end time&lt;/b&gt; for the temporal-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones for the temporal query. | 
 **options** | **str**| options parameter is used for obtaining the number of hits for a query | [optional] 
 **id** | **str**| id of the resource in catalogue | [optional] 
 **api** | **str**| Valid DX(Data exchange) api having base path as prefix appended to api | [optional] 
 **offset** | **int**| This parameter specifies the &lt;b&gt;offset&lt;/b&gt; for the read metering query. &lt;br/&gt; By default offset value is 0 and we can give offset value according our need. | [optional] [default to 0]
 **limit** | **int**| This parameter specifies the &lt;b&gt;limit&lt;/b&gt; for the read metering query. &lt;br/&gt; By default limit value is 2000 and we can give limit value according our need. | [optional] [default to 2000]

### Return type

[**ProviderAudit200Response**](ProviderAudit200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Data fetched successfully |  -  |
**204** | Empty Response |  -  |
**400** | Bad query - Missing/Invalid parameters |  -  |
**401** | - Unauthorized - &#x60;token&#x60; invalid/expired - Unauthorized - &#x60;clientId&#x60; &amp; &#x60;clientSecret&#x60; invalid/not match |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **overview**
> Overview200Response overview(starttime=starttime, endtime=endtime)

overview

Overview API is used to get count based on month. Without parameter it will return last 12 months count data. This responds the number of times the API requests are made from OGC Resource Server within the given time frame when the user specifies `starttime` and `endtime`.

### Example


```python
import openapi_client
from openapi_client.models.overview200_response import Overview200Response
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://ogc.iudx.io
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://ogc.iudx.io"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MeteringApi(api_client)
    starttime = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>start time</b> for the summary-query in `ISO8601` format. <br/> Data exchange currently accepts `IST` and `UTC` time zones. (optional)
    endtime = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>end time</b> for the summary-query in `ISO8601` format. <br/> Data exchange currently accepts `IST` and `UTC` time zones. (optional)

    try:
        # overview
        api_response = api_instance.overview(starttime=starttime, endtime=endtime)
        print("The response of MeteringApi->overview:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MeteringApi->overview: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **starttime** | **datetime**| This parameter specifies the &lt;b&gt;start time&lt;/b&gt; for the summary-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; Data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones. | [optional] 
 **endtime** | **datetime**| This parameter specifies the &lt;b&gt;end time&lt;/b&gt; for the summary-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; Data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones. | [optional] 

### Return type

[**Overview200Response**](Overview200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Overview details fetched successfully |  -  |
**400** | Bad request |  -  |
**401** | - Unauthorized - &#x60;token&#x60; invalid/expired - Unauthorized - &#x60;clientId&#x60; &amp; &#x60;clientSecret&#x60; invalid/not match |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **provider_audit**
> ProviderAudit200Response provider_audit(timerel, time, end_time, options=options, id=id, api=api, consumer=consumer, provider_id=provider_id, offset=offset, limit=limit)

provider search

A provider could use `/provider/audit` API to get the detailed summary of the resources with the APIs associated with provider. The count query gives the sum total of calls by the provider to the OGC Resource Server when the user provides `count` in the options.

### Example


```python
import openapi_client
from openapi_client.models.provider_audit200_response import ProviderAudit200Response
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://ogc.iudx.io
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://ogc.iudx.io"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MeteringApi(api_client)
    timerel = 'timerel_example' # str | The temporal relation type of <b>timerel</b> to be performed.
    time = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>start time</b> for the temporal-query in `ISO8601` format. <br/> data exchange currently accepts `IST` and `UTC` time zones for the temporal query.
    end_time = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>end time</b> for the temporal-query in `ISO8601` format. <br/> data exchange currently accepts `IST` and `UTC` time zones for the temporal query.
    options = 'options_example' # str | options parameter is used for obtaining the number of hits for a query (optional)
    id = 'id_example' # str | id of the resource in catalogue (optional)
    api = 'api_example' # str | Valid DX(Data exchange) api having base path as prefix appended to api (optional)
    consumer = 'consumer_example' # str | Id of consumer (optional)
    provider_id = 'provider_id_example' # str | Id of the provider (optional)
    offset = 0 # int | This parameter specifies the <b>offset</b> for the read metering query. <br/> By default offset value is 0 and we can give offset value according our need. (optional) (default to 0)
    limit = 2000 # int | This parameter specifies the <b>limit</b> for the read metering query. <br/> By default limit value is 2000 and we can give limit value according our need. (optional) (default to 2000)

    try:
        # provider search
        api_response = api_instance.provider_audit(timerel, time, end_time, options=options, id=id, api=api, consumer=consumer, provider_id=provider_id, offset=offset, limit=limit)
        print("The response of MeteringApi->provider_audit:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MeteringApi->provider_audit: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **timerel** | **str**| The temporal relation type of &lt;b&gt;timerel&lt;/b&gt; to be performed. | 
 **time** | **datetime**| This parameter specifies the &lt;b&gt;start time&lt;/b&gt; for the temporal-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones for the temporal query. | 
 **end_time** | **datetime**| This parameter specifies the &lt;b&gt;end time&lt;/b&gt; for the temporal-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones for the temporal query. | 
 **options** | **str**| options parameter is used for obtaining the number of hits for a query | [optional] 
 **id** | **str**| id of the resource in catalogue | [optional] 
 **api** | **str**| Valid DX(Data exchange) api having base path as prefix appended to api | [optional] 
 **consumer** | **str**| Id of consumer | [optional] 
 **provider_id** | **str**| Id of the provider | [optional] 
 **offset** | **int**| This parameter specifies the &lt;b&gt;offset&lt;/b&gt; for the read metering query. &lt;br/&gt; By default offset value is 0 and we can give offset value according our need. | [optional] [default to 0]
 **limit** | **int**| This parameter specifies the &lt;b&gt;limit&lt;/b&gt; for the read metering query. &lt;br/&gt; By default limit value is 2000 and we can give limit value according our need. | [optional] [default to 2000]

### Return type

[**ProviderAudit200Response**](ProviderAudit200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Data fetched successfully |  -  |
**204** | Empty Response |  -  |
**400** | Bad query - Missing/Invalid parameters |  -  |
**401** | - Unauthorized - &#x60;token&#x60; invalid/expired - Unauthorized - &#x60;clientId&#x60; &amp; &#x60;clientSecret&#x60; invalid/not match |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **summary**
> Summary200Response summary(starttime=starttime, endtime=endtime)

summary details

Summary API is used to get summary details and count for a given resource within the given time frame. The consumer could provide `starttime` and `endtime` and get the frequency usage of the resources.

### Example


```python
import openapi_client
from openapi_client.models.summary200_response import Summary200Response
from openapi_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to https://ogc.iudx.io
# See configuration.py for a list of all supported configuration parameters.
configuration = openapi_client.Configuration(
    host = "https://ogc.iudx.io"
)


# Enter a context with an instance of the API client
with openapi_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = openapi_client.MeteringApi(api_client)
    starttime = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>start time</b> for the summary-query in `ISO8601` format. <br/> Data exchange currently accepts `IST` and `UTC` time zones. (optional)
    endtime = '2013-10-20T19:20:30+01:00' # datetime | This parameter specifies the <b>end time</b> for the summary-query in `ISO8601` format. <br/> Data exchange currently accepts `IST` and `UTC` time zones. (optional)

    try:
        # summary details
        api_response = api_instance.summary(starttime=starttime, endtime=endtime)
        print("The response of MeteringApi->summary:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MeteringApi->summary: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **starttime** | **datetime**| This parameter specifies the &lt;b&gt;start time&lt;/b&gt; for the summary-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; Data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones. | [optional] 
 **endtime** | **datetime**| This parameter specifies the &lt;b&gt;end time&lt;/b&gt; for the summary-query in &#x60;ISO8601&#x60; format. &lt;br/&gt; Data exchange currently accepts &#x60;IST&#x60; and &#x60;UTC&#x60; time zones. | [optional] 

### Return type

[**Summary200Response**](Summary200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Summary details fetched successfully |  -  |
**204** | Empty Response |  -  |
**400** | Bad request |  -  |
**401** | - Unauthorized - &#x60;token&#x60; invalid/expired - Unauthorized - &#x60;clientId&#x60; &amp; &#x60;clientSecret&#x60; invalid/not match |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

