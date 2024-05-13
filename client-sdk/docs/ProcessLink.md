# ProcessLink


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**href** | **str** |  | [optional] 
**rel** | **str** |  | [optional] 
**type** | **str** |  | [optional] 
**title** | **str** |  | [optional] 

## Example

```python
from openapi_client.models.process_link import ProcessLink

# TODO update the JSON string below
json = "{}"
# create an instance of ProcessLink from a JSON string
process_link_instance = ProcessLink.from_json(json)
# print the JSON string representation of the object
print(ProcessLink.to_json())

# convert the object into a dict
process_link_dict = process_link_instance.to_dict()
# create an instance of ProcessLink from a dict
process_link_from_dict = ProcessLink.from_dict(process_link_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


