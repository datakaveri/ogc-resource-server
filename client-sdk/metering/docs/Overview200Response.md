# Overview200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**results** | **List[object]** | An array of objects | [optional] 

## Example

```python
from openapi_client.models.overview200_response import Overview200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Overview200Response from a JSON string
overview200_response_instance = Overview200Response.from_json(json)
# print the JSON string representation of the object
print(Overview200Response.to_json())

# convert the object into a dict
overview200_response_dict = overview200_response_instance.to_dict()
# create an instance of Overview200Response from a dict
overview200_response_from_dict = Overview200Response.from_dict(overview200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


