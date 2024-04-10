package ogc.rs.apiserver.router.util;

import java.util.ArrayList;
import java.util.List;
import io.vertx.core.json.JsonObject;

/**
 * Class to store fragments of OpenAPI spec JSON for the OGC spec and the STAC spec.
 */
public class OasFragments {

  private List<JsonObject> ogc;
  private List<JsonObject> stac;

  /**
   * Create a new {@link OasFragments} object. The OGC and STAC spec lists are initialized to
   * empty lists.
   */
  public OasFragments() {
    ogc = new ArrayList<JsonObject>();
    stac = new ArrayList<JsonObject>();
  }

  /**
   * Get list of OpenAPI JSON objects meant for the OGC spec.
   * 
   * @return list of {@link JsonObject}
   */
  public List<JsonObject> getOgc() {
    return new ArrayList<JsonObject>(ogc);
  }

  /**
   * Set list of OpenAPI JSON objects meant for the OGC spec.
   */
  public void setOgc(List<JsonObject> ogc) {
    this.ogc = new ArrayList<JsonObject>(ogc);
  }

  /**
   * Get list of OpenAPI JSON objects meant for the STAC spec.
   * 
   * @return list of {@link JsonObject}
   */
  public List<JsonObject> getStac() {
    return new ArrayList<JsonObject>(stac);
  }

  /**
   * Set list of OpenAPI JSON objects meant for the STAC spec.
   */
  public void setStac(List<JsonObject> stac) {
    this.stac = new ArrayList<JsonObject>(stac);
  }
}
