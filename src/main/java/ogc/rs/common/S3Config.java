package ogc.rs.common;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class to hold configuration options (bucket name, access key, secret key etc.) for a single S3 or
 * S3-compatible instance.
 * 
 */
public class S3Config {
  
  private String bucket;
  private String region;

  private String endpoint;

  private String accessKey;
  private String secretKey;

  private Boolean pathBasedAccess;
  private Boolean https;
  
  private S3Config(Builder b) {
    super();
    this.bucket = b.bucket;
    this.region = b.region;
    this.endpoint = b.endpoint;
    this.accessKey = b.accessKey;
    this.secretKey = b.secretKey;
    this.pathBasedAccess = b.pathBasedAccess;
    this.https = b.endpoint.startsWith("https");
  }
  
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

  /**
   * Class used to build an instance of {@link S3Config}.
   */
  public static class Builder {
    private static final Boolean DEFAULT_IS_PATH_BASED_ACCESS = false;
  
    private String bucket;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Boolean pathBasedAccess = DEFAULT_IS_PATH_BASED_ACCESS;

    /**
     * Set the bucket name for the S3 instance.
     * 
     * @param bucket
     * @return
     */
    public Builder bucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    /**
     * Set the region for the S3 instance.
     * 
     * @param region
     * @return
     */
    public Builder region(String region) {
      this.region = region;
      return this;
    }

    /**
     * Set the endpoint for the S3 instance. The endpoint must be in the format
     * [http/https]://[domain]:[port (optional)]
     * 
     * @param endpoint
     * @return
     */
    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }
    
    /**
     * Set the access key for the S3 instance.
     * 
     * @param accessKey
     * @return
     */
    public Builder accessKey(String accessKey) {
      this.accessKey = accessKey;
      return this;
    }
    
    /**
     * Set the secret key for the S3 instance.
     * 
     * @param secretKey
     * @return
     */
    public Builder secretKey(String secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    /**
     * Set whether the S3 instance is configured to operate with <a href=
     * "https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#path-style-access">path-based
     * access</a>. If the value is false <b>or is not set</b>, then it is considered to be <a href=
     * "https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#virtual-hosted-style-access">virtual-hosting
     * based access</a>.
     * 
     * @param pathBasedAccess
     * @return
     */
    public Builder pathBasedAccess(Boolean pathBasedAccess) {
      if (pathBasedAccess == null) {
        return this; // use default
      }

      this.pathBasedAccess = pathBasedAccess;
      return this;
    }
    
    /**
     * Build the {@link S3Config} object.
     * 
     * @return
     */
    public S3Config build() {
      validate();
      return new S3Config(this);
    }
    
    private void validate() throws IllegalArgumentException {
      if (bucket == null || region == null || endpoint == null || accessKey == null
          || secretKey == null) {
        throw new IllegalArgumentException(
            "Failed to initialize S3 config : one of bucket, region, endpoint, access key, secret key is null");
      }

      try {
        URI s = new URI(endpoint);
        if (!"https".equals(s.getScheme()) && !"http".equals(s.getScheme())) {
          throw new IllegalArgumentException("Failed to initialize S3 config : endpoint does not have http/https");
        }

      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Failed to initialize S3 config : endpoint is invalid");
      }

    }
  }
}
