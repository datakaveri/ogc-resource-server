package ogc.rs.apiserver.authorization.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen
public class Asset {
  String providerId;
  String itemId;
  String assetName;
  String assetType;
  String organizationId;
  String shortDescription;
  String accessPolicy;
  String createdAt;
  List<String> tags;

  public Asset() {
  }

  public Asset(JsonObject json) {
    AssetConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AssetConverter.toJson(this, json);
    return json;
  }

  public Asset(Asset other) {
    this.providerId = other.getProviderId();
    this.itemId = other.getItemId();
    this.assetName = other.getAssetName();
    this.assetType = other.getAssetType();
    this.organizationId = other.getOrganizationId();
    this.shortDescription = other.getShortDescription();
    this.accessPolicy = other.getAccessPolicy();
    this.tags = other.getTags();
    this.createdAt = other.getCreatedAt();
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public Asset setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public String getProviderId() {
    return providerId;
  }

  public Asset setProviderId(String providerId) {
    this.providerId = providerId;
    return this;
  }

  public String getItemId() {
    return itemId;
  }

  public Asset setItemId(String itemId) {
    this.itemId = itemId;
    return this;
  }

  public String getAssetName() {
    return assetName;
  }

  public Asset setAssetName(String assetName) {
    this.assetName = assetName;
    return this;
  }

  public String getAssetType() {
    return assetType;
  }

  public Asset setAssetType(String assetType) {
    this.assetType = assetType;
    return this;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public Asset setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
    return this;
  }

  public String getAccessPolicy() {
    return accessPolicy;
  }

  public Asset setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  public Asset setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public Asset setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public String toString() {
    return "Asset{" +
        "providerId='" + providerId + '\'' +
        ", itemId='" + itemId + '\'' +
        ", assetName='" + assetName + '\'' +
        ", assetType='" + assetType + '\'' +
        ", organizationId='" + organizationId + '\'' +
        ", shortDescription='" + shortDescription + '\'' +
        ", accessPolicy='" + accessPolicy + '\'' +
        ", tags = '" + tags + '\'' +
        ", createdAt = '" + createdAt + "'" +
        '}';
  }
}