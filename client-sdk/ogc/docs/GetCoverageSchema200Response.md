# GetCoverageSchema200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**var_schema** | **object** | The JSON schema of the coverage. | [optional] 

## Example

```python
from openapi_client.models.get_coverage_schema200_response import GetCoverageSchema200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetCoverageSchema200Response from a JSON string
get_coverage_schema200_response_instance = GetCoverageSchema200Response.from_json(json)
# print the JSON string representation of the object
print(GetCoverageSchema200Response.to_json())

# convert the object into a dict
get_coverage_schema200_response_dict = get_coverage_schema200_response_instance.to_dict()
# create an instance of GetCoverageSchema200Response from a dict
get_coverage_schema200_response_from_dict = GetCoverageSchema200Response.from_dict(get_coverage_schema200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


