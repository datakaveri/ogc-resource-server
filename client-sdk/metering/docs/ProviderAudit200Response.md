# ProviderAudit200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**results** | **List[object]** | An array of objects | [optional] 

## Example

```python
from openapi_client.models.provider_audit200_response import ProviderAudit200Response

# TODO update the JSON string below
json = "{}"
# create an instance of ProviderAudit200Response from a JSON string
provider_audit200_response_instance = ProviderAudit200Response.from_json(json)
# print the JSON string representation of the object
print(ProviderAudit200Response.to_json())

# convert the object into a dict
provider_audit200_response_dict = provider_audit200_response_instance.to_dict()
# create an instance of ProviderAudit200Response from a dict
provider_audit200_response_from_dict = ProviderAudit200Response.from_dict(provider_audit200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


