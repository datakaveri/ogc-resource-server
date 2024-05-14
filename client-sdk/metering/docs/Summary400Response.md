# Summary400Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**details** | **str** |  | [optional] 

## Example

```python
from openapi_client.models.summary400_response import Summary400Response

# TODO update the JSON string below
json = "{}"
# create an instance of Summary400Response from a JSON string
summary400_response_instance = Summary400Response.from_json(json)
# print the JSON string representation of the object
print(Summary400Response.to_json())

# convert the object into a dict
summary400_response_dict = summary400_response_instance.to_dict()
# create an instance of Summary400Response from a dict
summary400_response_from_dict = Summary400Response.from_dict(summary400_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


