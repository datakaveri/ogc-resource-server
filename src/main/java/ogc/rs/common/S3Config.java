package ogc.rs.common;

import java.net.URI;
import java.net.URISyntaxException;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Class to hold configuration options (bucket name, access key, secret key etc.) for a single S3 or
 * S3-compatible instance.
 * 
 */
@DataObject
@JsonGen
public class S3Config {
  
  private static final Boolean DEFAULT_IS_PATH_BASED_ACCESS = false;

  private String bucket;
  private String region;

  private String endpoint;

  private String accessKey;
  private String secretKey;
  
  private S3BucketReadAccess readAccess;

  private Boolean pathBasedAccess;
  private Boolean https;
  
  /**
   * Returns the configured bucket name.
   * 
   * @return name of bucket
   */
  public String getBucket() {
    return bucket;
  }

  /**
   * Returns the configured region.
   * 
   * @return name of region
   */
  public String getRegion() {
    return region;
  }

  /**
   * Returns the configured endpoint/URL. The endpoint format is [http/https]://[domain]:[port
   * (optional)].
   * 
   * @return the endpoint
   */
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * Returns the configured access key.
   * 
   * @return the access key
   */
  public String getAccessKey() {
    return accessKey;
  }

  /**
   * Returns the configured secret key.
   * 
   * @return the secret key
   */
  public String getSecretKey() {
    return secretKey;
  }
  
  /**
   * Returns whether the S3 instance is configured to operate with <a href=
   * "https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#path-style-access">path-based
   * access</a>. If the value is false, then it is considered to be <a href=
   * "https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#virtual-hosted-style-access">virtual-hosting
   * based access</a>.
   * 
   * @return boolean if S3 instance is set to work with path based access
   */
  public Boolean isPathBasedAccess() {
    return pathBasedAccess;
  }

  /**
   * Returns whether the S3 instance (i.e. the configured endpoint) works over HTTPS.
   * 
   * @return if S3 instance works over HTTPS
   */
  public Boolean isHttps() {
    return https;
  }

  public Boolean getPathBasedAccess() {
    return pathBasedAccess;
  }

  public void setPathBasedAccess(Boolean pathBasedAccess) {
    this.pathBasedAccess = pathBasedAccess;
  }

  public Boolean getHttps() {
    return https;
  }

  public void setHttps(Boolean https) {
    this.https = https;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
  
  public S3BucketReadAccess getReadAccess() {
    return readAccess;
  }

  public void setReadAccess(S3BucketReadAccess readAccess) {
    this.readAccess = readAccess;
  }

  public S3Config(JsonObject json) {
    S3ConfigConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    S3ConfigConverter.toJson(this, json);
    return json;
  }
}
