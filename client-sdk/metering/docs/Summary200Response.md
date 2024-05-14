# Summary200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**results** | **List[object]** | An array of objects | [optional] 

## Example

```python
from openapi_client.models.summary200_response import Summary200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Summary200Response from a JSON string
summary200_response_instance = Summary200Response.from_json(json)
# print the JSON string representation of the object
print(Summary200Response.to_json())

# convert the object into a dict
summary200_response_dict = summary200_response_instance.to_dict()
# create an instance of Summary200Response from a dict
summary200_response_from_dict = Summary200Response.from_dict(summary200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


