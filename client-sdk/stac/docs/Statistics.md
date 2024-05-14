# Statistics

By default, only ranges with a minimum and a maximum value can be specified. Ranges can be specified for ordinal values only, which means they need to have a rank order. Therefore, ranges can only be specified for numbers and some special types of strings. Examples: grades (A to F), dates or times. Implementors are free to add other derived statistical values to the object, for example `mean` or `stddev`.

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**min** | [**StatisticsMin**](StatisticsMin.md) |  | 
**max** | [**StatisticsMin**](StatisticsMin.md) |  | 

## Example

```python
from openapi_client.models.statistics import Statistics

# TODO update the JSON string below
json = "{}"
# create an instance of Statistics from a JSON string
statistics_instance = Statistics.from_json(json)
# print the JSON string representation of the object
print(Statistics.to_json())

# convert the object into a dict
statistics_dict = statistics_instance.to_dict()
# create an instance of Statistics from a dict
statistics_from_dict = Statistics.from_dict(statistics_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


