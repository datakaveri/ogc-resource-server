# GetLandingPage200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**title** | **str** |  | [optional] 
**description** | **str** |  | [optional] 
**links** | [**List[GetLandingPage200ResponseLinksInner]**](GetLandingPage200ResponseLinksInner.md) |  | 

## Example

```python
from openapi_client.models.get_landing_page200_response import GetLandingPage200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetLandingPage200Response from a JSON string
get_landing_page200_response_instance = GetLandingPage200Response.from_json(json)
# print the JSON string representation of the object
print(GetLandingPage200Response.to_json())

# convert the object into a dict
get_landing_page200_response_dict = get_landing_page200_response_instance.to_dict()
# create an instance of GetLandingPage200Response from a dict
get_landing_page200_response_from_dict = GetLandingPage200Response.from_dict(get_landing_page200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


