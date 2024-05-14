# CrsOneOf


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**uri** | **str** | Reference to one coordinate reference system (CRS) | 
**wkt** | **object** |  | 
**reference_system** | **object** | A reference system data structure as defined in the MD_ReferenceSystem of the ISO 19115 | 

## Example

```python
from openapi_client.models.crs_one_of import CrsOneOf

# TODO update the JSON string below
json = "{}"
# create an instance of CrsOneOf from a JSON string
crs_one_of_instance = CrsOneOf.from_json(json)
# print the JSON string representation of the object
print(CrsOneOf.to_json())

# convert the object into a dict
crs_one_of_dict = crs_one_of_instance.to_dict()
# create an instance of CrsOneOf from a dict
crs_one_of_from_dict = CrsOneOf.from_dict(crs_one_of_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


