# CatalogLandingPage


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | 
**title** | **str** |  | [optional] 
**type** | **str** |  | 
**description** | **str** |  | 
**links** | [**List[CatalogLandingPageLinksInner]**](CatalogLandingPageLinksInner.md) |  | 
**stac_version** | **str** |  | 
**stac_extensions** | [**List[CatalogLandingPageStacExtensionsInner]**](CatalogLandingPageStacExtensionsInner.md) |  | [optional] 

## Example

```python
from openapi_client.models.catalog_landing_page import CatalogLandingPage

# TODO update the JSON string below
json = "{}"
# create an instance of CatalogLandingPage from a JSON string
catalog_landing_page_instance = CatalogLandingPage.from_json(json)
# print the JSON string representation of the object
print(CatalogLandingPage.to_json())

# convert the object into a dict
catalog_landing_page_dict = catalog_landing_page_instance.to_dict()
# create an instance of CatalogLandingPage from a dict
catalog_landing_page_from_dict = CatalogLandingPage.from_dict(catalog_landing_page_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


