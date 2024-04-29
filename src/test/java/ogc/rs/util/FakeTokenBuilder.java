package ogc.rs.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.PasswordLookup;
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
  private JsonObject cons;
  private String sub;
  private String aud;
  private String role;
  private static JWSSigner signer;
  private UUID did;
  private String drl;

  private UUID rg;

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

  public FakeTokenBuilder withCons(JsonObject cons) {
    this.cons = cons;
    return this;
  }

  public FakeTokenBuilder withSub(String sub) {
    this.sub = sub;
    return this;
  }

  public FakeTokenBuilder withAud() throws IOException {
    String configFilePath = "secrets/configs/dev.json";
    byte[] jsonData = Files.readAllBytes(Paths.get(configFilePath));
    JsonObject configJson = new JsonObject(new String(jsonData, "UTF-8"));
    JsonArray modules = configJson.getJsonArray("modules");
    for (Object module : modules) {
      if (module instanceof JsonObject) {
        JsonObject moduleJson = (JsonObject) module;
        if ("ogc.rs.authenticator.AuthenticationVerticle".equals(moduleJson.getString("id"))
            && moduleJson.containsKey("audience")) {
          this.aud = moduleJson.getString("audience");
          LOGGER.debug("Audience Value : {} ", aud);
          break;
        }
      }
    }
    return this;
  }

  public FakeTokenBuilder withRole(String role) {
    this.role = role;
    return this;
  }

  public FakeTokenBuilder withResource(UUID resourceId, UUID resourceGroupId) {
    this.iid = "ri:" + resourceId;
    this.rg = resourceGroupId;
    return this;
  }

  public FakeTokenBuilder withResourceServer(String resourceServerId) {
    this.iid = "rs:" + resourceServerId;
    return this;
  }

  public FakeTokenBuilder withDelegate(UUID did, String drl) {
    if (!"provider".equalsIgnoreCase(role) && !"consumer".equalsIgnoreCase(role)) {
      this.did = did;
      this.drl = drl;
      this.role = "delegate";
    } else {
      throw new IllegalArgumentException("Invalid role: Role must be 'delegate'");
    }

    return this;
  }

  public String build() {

    LOGGER.debug("building the token");
    Date currentTime = new Date();
    long expiry = currentTime.getTime() + (5 * 60 * 1000);
    Date expirationTime = new Date(expiry);

    try {

      if (role == null || role.isEmpty() || iid == null || iid.isEmpty()) {
        throw new IllegalArgumentException("Role or IID is not set");
      }

      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(sub)
              .issuer("fakeauth.ugix.io")
              .audience(aud)
              .expirationTime(expirationTime)
              .issueTime(currentTime)
              .claim("iid", iid)
              .claim("role", role)
              .claim("cons", Map.of("access", List.of("api")))
              .build();
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
