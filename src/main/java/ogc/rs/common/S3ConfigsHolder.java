package ogc.rs.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
/**
 * Class to hold a set of S3 bucket configurations that are picked up from the server config.
 */
public class S3ConfigsHolder {

  public static final String S3_CONFIGS_BLOCK_KEY_NAME = "s3BucketsConfig";
  public static final String DEFAULT_BUCKET_IDENTIFIER = "default";


  private static final String BUCKET_CONF_OP = "bucket";
  private static final String REGION_CONF_OP = "region";
  private static final String ENDPOINT_CONF_OP = "endpoint";
  private static final String ACCESS_KEY_CONF_OP = "accessKey";
  private static final String SECRET_KEY_CONF_OP = "secretKey";
  private static final String READ_ACCESS_CONF_OP = "readAccess";
  private static final String PATH_BASED_ACC_CONF_OP = "pathBasedAccess";

  private JsonObject configs;
  private JsonObject identifierAndReadAccess;

  /**
   * Form {@link S3ConfigsHolder} object from JSON object. Each key in the object would represent a
   * different bucket. If forming it from server config, use
   * {@link S3ConfigsHolder#S3_CONFIGS_BLOCK_KEY_NAME} when fetching object.
   * 
   * @param configsObj
   * @return
   * @throws IllegalArgumentException
   */
  public static S3ConfigsHolder createFromServerConfig(JsonObject configsObj)
      throws IllegalArgumentException {

    Map<String, S3Config> map = new HashMap<String, S3Config>();

    if (configsObj.isEmpty()) {
      throw new IllegalArgumentException("S3 config object is empty");
    }

    Iterator<Entry<String, Object>> it = configsObj.iterator();

    while (it.hasNext()) {
      Entry<String, Object> etr = it.next();

      if (!(etr.getValue() instanceof JsonObject)) {
        throw new IllegalArgumentException("S3 config block not a JSON Object");
      }

      JsonObject obj = (JsonObject) etr.getValue();
      validate(etr.getKey(), obj);
      map.put(etr.getKey(), new S3Config(obj));
    }

    return new S3ConfigsHolder(configsObj);
  }

  public S3ConfigsHolder(JsonObject obj) {
    this.configs = obj;
    this.identifierAndReadAccess = new JsonObject();
    obj.stream().map(i -> identifierAndReadAccess.put(i.getKey(),
        ((JsonObject) i.getValue()).getBoolean(READ_ACCESS_CONF_OP)));
  }

  public JsonObject toJson() {
    return configs;
  }

  /**
   * Returns all bucket identifiers and the read access for each in a JSON object.
   * 
   * @return
   */
  public JsonObject listAllIdentifiers() {
    return identifierAndReadAccess;
  }

  /**
   * Get config for an S3 bucket from it's identifier (the key used for the block in the config).
   * Since an {@link Optional} is returned, the {@link Optional#isEmpty()} method must be used
   * before calling {@link Optional#get()}.
   * 
   * @param identifier
   * @return
   */
  public Optional<S3Config> getConfigByIdentifier(String identifier) {
    if (configs.containsKey(identifier)) {
      return Optional.of(new S3Config(configs.getJsonObject(identifier)));
    }

    return Optional.empty();
  }

  /**
   * Validate an S3 config JSON to make sure all required fields are present.
   * 
   * @param identifier the string identifier for the config
   * @param obj the config in JSON
   * @throws IllegalArgumentException
   */
  private static void validate(String identifier, JsonObject obj) throws IllegalArgumentException {

    if (!obj.fieldNames().containsAll(List.of(BUCKET_CONF_OP, ACCESS_KEY_CONF_OP, ENDPOINT_CONF_OP,
        REGION_CONF_OP, SECRET_KEY_CONF_OP, READ_ACCESS_CONF_OP, PATH_BASED_ACC_CONF_OP))) {

      throw new IllegalArgumentException("Failed to initialize S3 config for identifier '"
          + identifier + "' : atleast one of bucket, region, endpoint, access key"
          + ", secret key, path based access, read access is missing");
    }
    List<String> strValues = List
        .of(BUCKET_CONF_OP, ACCESS_KEY_CONF_OP, ENDPOINT_CONF_OP, REGION_CONF_OP,
            SECRET_KEY_CONF_OP)
        .stream().filter(i -> obj.getString(i).isEmpty() || obj.getValue(i) == null)
        .collect(Collectors.toList());

    if (!strValues.isEmpty()) {

      throw new IllegalArgumentException("Failed to initialize S3 config for identifier '"
          + identifier
          + "' : atleast one of bucket, region, endpoint, access key, secret key is empty/null");
    }

    if (obj.getValue(PATH_BASED_ACC_CONF_OP) instanceof Boolean) {
      throw new IllegalArgumentException("Failed to initialize S3 config for identifier '"
          + identifier + "' : " + PATH_BASED_ACC_CONF_OP + " is not a boolean");
    }

    try {
      S3BucketReadAccess.valueOf(obj.getString(READ_ACCESS_CONF_OP).toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Failed to initialize S3 config for identifier '"
          + identifier + "' : " + "read access should be either open, secure");
    }

    try {
      URI s = new URI(obj.getString(ENDPOINT_CONF_OP));
      if (!"https".equals(s.getScheme()) && !"http".equals(s.getScheme())) {
        throw new IllegalArgumentException(
            "Failed to initialize S3 config : endpoint does not have http/https");
      }

    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Failed to initialize S3 config : endpoint is invalid");
    }

  }

}
