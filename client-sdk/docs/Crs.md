# Crs


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**uri** | **str** | Reference to one coordinate reference system (CRS) | 
**wkt** | **object** |  | 
**reference_system** | **object** | A reference system data structure as defined in the MD_ReferenceSystem of the ISO 19115 | 

## Example

```python
from openapi_client.models.crs import Crs

# TODO update the JSON string below
json = "{}"
# create an instance of Crs from a JSON string
crs_instance = Crs.from_json(json)
# print the JSON string representation of the object
print(Crs.to_json())

# convert the object into a dict
crs_dict = crs_instance.to_dict()
# create an instance of Crs from a dict
crs_from_dict = Crs.from_dict(crs_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


