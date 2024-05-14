# StacCollectionSummariesValue


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**min** | [**StatisticsMin**](StatisticsMin.md) |  | 
**max** | [**StatisticsMin**](StatisticsMin.md) |  | 

## Example

```python
from openapi_client.models.stac_collection_summaries_value import StacCollectionSummariesValue

# TODO update the JSON string below
json = "{}"
# create an instance of StacCollectionSummariesValue from a JSON string
stac_collection_summaries_value_instance = StacCollectionSummariesValue.from_json(json)
# print the JSON string representation of the object
print(StacCollectionSummariesValue.to_json())

# convert the object into a dict
stac_collection_summaries_value_dict = stac_collection_summaries_value_instance.to_dict()
# create an instance of StacCollectionSummariesValue from a dict
stac_collection_summaries_value_from_dict = StacCollectionSummariesValue.from_dict(stac_collection_summaries_value_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


