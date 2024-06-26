# coding: utf-8

"""
    OGC Compliant IUDX Resource Server

    OGC compliant Features and Common API definitions. Includes Schema and Response Objects.

    The version of the OpenAPI document: 1.0.1
    Contact: info@iudx.org.in
    Generated by OpenAPI Generator (https://openapi-generator.tech)

    Do not edit the class manually.
"""  # noqa: E501


from __future__ import annotations
import pprint
import re  # noqa: F401
import json

from pydantic import BaseModel, ConfigDict, Field, StrictStr
from typing import Any, ClassVar, Dict, List, Optional
from openapi_client.models.catalog_landing_page_links_inner import CatalogLandingPageLinksInner
from openapi_client.models.catalog_landing_page_stac_extensions_inner import CatalogLandingPageStacExtensionsInner
from openapi_client.models.stac_collection_extent import StacCollectionExtent
from openapi_client.models.stac_collection_summaries_value import StacCollectionSummariesValue
from typing import Optional, Set
from typing_extensions import Self

class StacCollection(BaseModel):
    """
    StacCollection
    """ # noqa: E501
    id: StrictStr = Field(description="identifier of the collection used, for example, in URIs")
    title: Optional[StrictStr] = Field(default=None, description="human readable title of the collection")
    type: StrictStr
    description: StrictStr = Field(description="Detailed multi-line description to fully explain the catalog or collection. [CommonMark 0.29](http://commonmark.org/) syntax MAY be used for rich text representation.")
    links: List[CatalogLandingPageLinksInner]
    stac_version: StrictStr
    stac_extensions: Optional[List[CatalogLandingPageStacExtensionsInner]] = None
    keywords: Optional[List[StrictStr]] = Field(default=None, description="List of keywords describing the collection.")
    license: StrictStr = Field(description="License(s) of the data as a SPDX [License identifier](https://spdx.org/licenses/). Alternatively, use `proprietary` if the license is not on the SPDX license list or `various` if multiple licenses apply. In these two cases links to the license texts SHOULD be added, see the `license` link relation type.  Non-SPDX licenses SHOULD add a link to the license text with the `license` relation in the links section. The license text MUST NOT be provided as a value of this field. If there is no public license URL available, it is RECOMMENDED to host the license text and link to it.")
    extent: StacCollectionExtent
    summaries: Optional[Dict[str, StacCollectionSummariesValue]] = Field(default=None, description="Summaries are either a unique set of all available values *or* statistics. Statistics by default only specify the range (minimum and maximum values), but can optionally be accompanied by additional statistical values. The range can specify the potential range of values, but it is recommended to be as precise as possible. The set of values must contain at least one element and it is strongly recommended to list all values. It is recommended to list as many properties as reasonable so that consumers get a full overview of the Collection. Properties that are covered by the Collection specification (e.g. `providers` and `license`) may not be repeated in the summaries.")
    __properties: ClassVar[List[str]] = ["id", "title", "type", "description", "links", "stac_version", "stac_extensions", "keywords", "license", "extent", "summaries"]

    model_config = ConfigDict(
        populate_by_name=True,
        validate_assignment=True,
        protected_namespaces=(),
    )


    def to_str(self) -> str:
        """Returns the string representation of the model using alias"""
        return pprint.pformat(self.model_dump(by_alias=True))

    def to_json(self) -> str:
        """Returns the JSON representation of the model using alias"""
        # TODO: pydantic v2: use .model_dump_json(by_alias=True, exclude_unset=True) instead
        return json.dumps(self.to_dict())

    @classmethod
    def from_json(cls, json_str: str) -> Optional[Self]:
        """Create an instance of StacCollection from a JSON string"""
        return cls.from_dict(json.loads(json_str))

    def to_dict(self) -> Dict[str, Any]:
        """Return the dictionary representation of the model using alias.

        This has the following differences from calling pydantic's
        `self.model_dump(by_alias=True)`:

        * `None` is only added to the output dict for nullable fields that
          were set at model initialization. Other fields with value `None`
          are ignored.
        """
        excluded_fields: Set[str] = set([
        ])

        _dict = self.model_dump(
            by_alias=True,
            exclude=excluded_fields,
            exclude_none=True,
        )
        # override the default output from pydantic by calling `to_dict()` of each item in links (list)
        _items = []
        if self.links:
            for _item in self.links:
                if _item:
                    _items.append(_item.to_dict())
            _dict['links'] = _items
        # override the default output from pydantic by calling `to_dict()` of each item in stac_extensions (list)
        _items = []
        if self.stac_extensions:
            for _item in self.stac_extensions:
                if _item:
                    _items.append(_item.to_dict())
            _dict['stac_extensions'] = _items
        # override the default output from pydantic by calling `to_dict()` of extent
        if self.extent:
            _dict['extent'] = self.extent.to_dict()
        # override the default output from pydantic by calling `to_dict()` of each value in summaries (dict)
        _field_dict = {}
        if self.summaries:
            for _key in self.summaries:
                if self.summaries[_key]:
                    _field_dict[_key] = self.summaries[_key].to_dict()
            _dict['summaries'] = _field_dict
        return _dict

    @classmethod
    def from_dict(cls, obj: Optional[Dict[str, Any]]) -> Optional[Self]:
        """Create an instance of StacCollection from a dict"""
        if obj is None:
            return None

        if not isinstance(obj, dict):
            return cls.model_validate(obj)

        _obj = cls.model_validate({
            "id": obj.get("id"),
            "title": obj.get("title"),
            "type": obj.get("type"),
            "description": obj.get("description"),
            "links": [CatalogLandingPageLinksInner.from_dict(_item) for _item in obj["links"]] if obj.get("links") is not None else None,
            "stac_version": obj.get("stac_version"),
            "stac_extensions": [CatalogLandingPageStacExtensionsInner.from_dict(_item) for _item in obj["stac_extensions"]] if obj.get("stac_extensions") is not None else None,
            "keywords": obj.get("keywords"),
            "license": obj.get("license"),
            "extent": StacCollectionExtent.from_dict(obj["extent"]) if obj.get("extent") is not None else None,
            "summaries": dict(
                (_k, StacCollectionSummariesValue.from_dict(_v))
                for _k, _v in obj["summaries"].items()
            )
            if obj.get("summaries") is not None
            else None
        })
        return _obj


