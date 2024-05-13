package ogc.rs.util;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.PasswordLookup;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.nimbusds.jose.jwk.JWKSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;

public class FakeTokenBuilder {
  private static final Logger LOGGER = LogManager.getLogger(FakeTokenBuilder.class);

  private String iid;
  private JsonObject cons = new JsonObject();
  private UUID sub = UUID.randomUUID();
  private String role;
  private static JWSSigner signer;
  private UUID did;
  private String drl;

  private UUID rg;
  private static final String iss = "fakeauth.ugix.io";
  public static final String aud = "ogc.server.integration-test.com";

  // Static initialization block for setting up the signer and audience

  static {
    KeyStore keyStore = null;
    try {
      keyStore = KeyStore.getInstance("jks");
      FileInputStream fis = new FileInputStream("src/test/resources/keystore-ec.jks");
      keyStore.load(fis, "secret".toCharArray());
      JWKSet jwkSet =
          JWKSet.load(
              keyStore,
              new PasswordLookup() {
                @Override
                public char[] lookupPassword(String s) {
                  return "secret".toCharArray();
                }
              });
      LOGGER.debug("jwkSET" + jwkSet.getKeys().get(0).toECKey().toECPrivateKey());
      ECKey ecPrivateKey = (ECKey) jwkSet.getKeys().get(0);
      signer = new ECDSASigner(ecPrivateKey);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the 'cons' claim for the JWT token.
   *
   * @param cons JsonObject representing the 'constraints'
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withCons(JsonObject cons) {
    this.cons = cons;
    return this;
  }

  /**
   * Sets the 'sub' (subject) claim for the JWT token.
   *
   * @param sub UUID representing the 'sub' claim
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withSub(UUID sub) {
    this.sub = sub;
    return this;
  }

  /**
   * Sets the 'iid' and 'rg' claims for the JWT token.
   *
   * @param resourceId UUID representing the resource ID
   * @param resourceGroupId UUID representing the resource group ID
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withResourceAndRg(UUID resourceId, UUID resourceGroupId) {
    this.iid = "ri:" + resourceId;
    this.rg = resourceGroupId;
    return this;
  }

  public FakeTokenBuilder withResource(UUID resourceId) {
    this.iid = "ri:" + resourceId;
    return this;
  }

  /**
   * Sets the 'iid' claim for the JWT token for a resource server.
   * Its same as server name and derived from config.
   *
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withResourceServer() {
    this.iid = "rs:" + aud;
    return this;
  }

  /**
   * Sets the 'role' claim to 'consumer'.
   *
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withRoleConsumer() {
    this.role = "consumer";
    return this;
  }

  /**
   * Sets the 'role' claim to 'provider'.
   *
   * @return FakeTokenBuilder instance
   */
  public FakeTokenBuilder withRoleProvider() {
    this.role = "provider";
    return this;
  }

  /**
   * Sets the 'did' (delegate ID) and 'drl' (delegate role) claims for the JWT token.
   *
   * @param did UUID representing the delegate ID
   * @param drl String representing the delegate role ('provider' or 'consumer')
   * @return FakeTokenBuilder instance
   * @throws IllegalArgumentException if an invalid 'drl' value is provided
   */
  public FakeTokenBuilder withDelegate(UUID did, String drl) {
    if ("provider".equalsIgnoreCase(drl) || "consumer".equalsIgnoreCase(drl)) {
      this.did = did;
      this.drl = drl;
      this.role = "delegate";
    } else {
      throw new IllegalArgumentException("Invalid drl value : It should be provider or delegate");
    }

    return this;
  }

  /**
   * Builds and returns the signed JWT token based on the configured claims.
   *
   * @return String representing the serialized signed JWT token
   * @throws RuntimeException if an error occurs during JWT token creation
   */
  public String build() {

    LOGGER.debug("building the token");
    Date currentTime = new Date();
    long expiry = currentTime.getTime() + (5 * 60 * 1000);
    Date expirationTime = new Date(expiry);

    try {

      if (role == null || role.isEmpty() || iid == null || iid.isEmpty()) {
        throw new IllegalArgumentException("Role or IID is not set");
      }

      JWTClaimsSet.Builder claimsSetBuilder =
          new JWTClaimsSet.Builder()
              .subject(String.valueOf(sub))
              .issuer(iss)
              .audience(aud)
              .expirationTime(expirationTime)
              .issueTime(currentTime)
              .claim("iid", iid)
              .claim("role", role)
              .claim("cons", JSONObjectUtils.parse(cons.toString()));

      // Conditionally add fields based on role
      if ("delegate".equals(role)) {
        claimsSetBuilder.claim("drl", drl);
        claimsSetBuilder.claim("did", did);
      }

      if (rg!= null) {
        claimsSetBuilder.claim("rg", rg);
      }

      JWTClaimsSet claimsSet = claimsSetBuilder.build();
      SignedJWT signedJWT =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(),
              claimsSet);
      signedJWT.sign(signer);
      return signedJWT.serialize();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
