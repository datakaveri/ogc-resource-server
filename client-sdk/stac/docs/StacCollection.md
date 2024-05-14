# StacCollection


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** | identifier of the collection used, for example, in URIs | 
**title** | **str** | human readable title of the collection | [optional] 
**type** | **str** |  | 
**description** | **str** | Detailed multi-line description to fully explain the catalog or collection. [CommonMark 0.29](http://commonmark.org/) syntax MAY be used for rich text representation. | 
**links** | [**List[CatalogLandingPageLinksInner]**](CatalogLandingPageLinksInner.md) |  | 
**stac_version** | **str** |  | 
**stac_extensions** | [**List[CatalogLandingPageStacExtensionsInner]**](CatalogLandingPageStacExtensionsInner.md) |  | [optional] 
**keywords** | **List[str]** | List of keywords describing the collection. | [optional] 
**license** | **str** | License(s) of the data as a SPDX [License identifier](https://spdx.org/licenses/). Alternatively, use &#x60;proprietary&#x60; if the license is not on the SPDX license list or &#x60;various&#x60; if multiple licenses apply. In these two cases links to the license texts SHOULD be added, see the &#x60;license&#x60; link relation type.  Non-SPDX licenses SHOULD add a link to the license text with the &#x60;license&#x60; relation in the links section. The license text MUST NOT be provided as a value of this field. If there is no public license URL available, it is RECOMMENDED to host the license text and link to it. | 
**extent** | [**StacCollectionExtent**](StacCollectionExtent.md) |  | 
**summaries** | [**Dict[str, StacCollectionSummariesValue]**](StacCollectionSummariesValue.md) | Summaries are either a unique set of all available values *or* statistics. Statistics by default only specify the range (minimum and maximum values), but can optionally be accompanied by additional statistical values. The range can specify the potential range of values, but it is recommended to be as precise as possible. The set of values must contain at least one element and it is strongly recommended to list all values. It is recommended to list as many properties as reasonable so that consumers get a full overview of the Collection. Properties that are covered by the Collection specification (e.g. &#x60;providers&#x60; and &#x60;license&#x60;) may not be repeated in the summaries. | [optional] 

## Example

```python
from openapi_client.models.stac_collection import StacCollection

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollection from a JSON string
stac_collection_instance = StacCollection.from_json(json)
# print the JSON string representation of the object
print(StacCollection.to_json())

# convert the object into a dict
stac_collection_dict = stac_collection_instance.to_dict()
# create an instance of StacCollection from a dict
stac_collection_from_dict = StacCollection.from_dict(stac_collection_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


