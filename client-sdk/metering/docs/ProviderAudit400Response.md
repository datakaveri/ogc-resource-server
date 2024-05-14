# ProviderAudit400Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 
**details** | **str** |  | [optional] 

## Example

```python
from openapi_client.models.provider_audit400_response import ProviderAudit400Response

# TODO update the JSON string below
json = "{}"
# create an instance of ProviderAudit400Response from a JSON string
provider_audit400_response_instance = ProviderAudit400Response.from_json(json)
# print the JSON string representation of the object
print(ProviderAudit400Response.to_json())

# convert the object into a dict
provider_audit400_response_dict = provider_audit400_response_instance.to_dict()
# create an instance of ProviderAudit400Response from a dict
provider_audit400_response_from_dict = ProviderAudit400Response.from_dict(provider_audit400_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


