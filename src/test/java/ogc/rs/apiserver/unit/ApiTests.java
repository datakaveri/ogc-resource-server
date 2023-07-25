package ogc.rs.apiserver.unit;

import io.reactiverse.junit5.web.WebClientOptionsInject;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import static io.reactiverse.junit5.web.TestRequest.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class})
public class ApiTests {
  // move test port to the Constants file
  private static final int OGC_TEST_PORT = 5010;
  private static Vertx vertxObj;
  static JsonObject config;
  static boolean deployOnlyApi;

  @WebClientOptionsInject
  public WebClientOptions options =
          new WebClientOptions().setDefaultHost("localhost").setDefaultPort(OGC_TEST_PORT);


  @BeforeAll
  @DisplayName("Reading test configuration")
  static void startServer(Vertx vertx, VertxTestContext testContext){
    vertxObj = vertx;
    final String TEST_CONFIG_PATH = "./secrets/configs/test.json";
    try {
      config = new JsonObject(Files.readString(Paths.get(TEST_CONFIG_PATH)));
    }catch (FileNotFoundException e) {
      testContext.failNow(new Throwable(e.getMessage()));
    } catch (IOException e) {
      testContext.failNow(new Throwable("Couldn't read the configuration file!\nDetail:", e.getCause()));
    }
    if (config.isEmpty()){
      testContext.failNow("Configuration file is empty!");
    }
    // initialise the deployer???
    deployOnlyApi = true;
    testContext.completeNow();
  }

  // TODO: This logic will be moved to @BeforeAll once all the APIs have been finished
  @BeforeEach
  @DisplayName("Deploying individual Verticles as per tests")
  void deployingVerticles(Vertx vertx, VertxTestContext testContext){
    if(deployOnlyApi)
      vertxObj.deployVerticle(config.getJsonObject("apiserver").getString("id")
          , new DeploymentOptions().setInstances(config.getJsonObject("apiserver").getInteger("verticleInstances"))
                  .setConfig(config))
          .onSuccess(success -> testContext.completeNow())
          .onFailure(fail -> testContext.failNow(fail.getMessage()));
    else {
      testContext.completeNow();
    }
  }

  @AfterAll
  public static void finish(Vertx vertx, VertxTestContext testContext) {
    vertx.close().onSuccess(success -> testContext.completeNow())
        .onFailure(failure -> testContext.failNow(failure.getMessage()));
  }
  @Test
  @DisplayName("/ (landing page) api test")
  void testLandingPage(Vertx vertx, VertxTestContext testContext, WebClient webClient){
    // check for 200 status
    // links (required) to -->
    // openapi specs
    // conformance class
    // collections
    // should follow link.yml -->
    // {"href": <links>, "rel": , "self"/"alternate"/"data"/"service-desc"/"conformance", "type": json, "title":}
    /*
      / or landing page should have the following requirements-
      /- reference to itself
      /api - defining openapi specs
      /conformance- defining the conformance classes
      /collections- feature collection link
      The links itself follows a schema with following requirements-
      - href - the link itself
      - rel - relation of the link with respect to the landing page. ex. root, self, service-desc, alternate, data
     */

    testRequest(webClient, HttpMethod.GET, "/")
        .with(requestHeader("Authorization", "Bearer token-string"))
        .expect(statusCode(200))
        .expect(response -> {
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("links"));
          assertFalse(responseBody.getJsonArray("links").isEmpty());
          // TODO: use Java8 stream API
          for (Object obj: responseBody.getJsonArray("links")){
            JsonObject jsonObj = (JsonObject) obj;
            assertFalse(jsonObj.isEmpty());
            assertTrue(jsonObj.containsKey("href") && jsonObj.containsKey("rel"));

            HashMap<String, HashSet<String>> expected = new HashMap<>();
            HashSet<String> expectedHref = new HashSet<>();
            expectedHref.add("http://localhost/");
            expectedHref.add("http://localhost/conformance");
            expectedHref.add("http://localhost/api");
            expectedHref.add("http://localhost/collections");

            HashSet<String> expectedRelations = new HashSet<>();
            expectedRelations.add("service-desc");
            expectedRelations.add("conformance");
            expectedRelations.add("self");
            expectedRelations.add("data");

            expected.put("href", expectedHref);
            expected.put("reFailed to resolve parameter [io.vertx.core.Vertx arg0] in methodl", expectedRelations);

            HashSet<String> actualHref = new HashSet<>();
            HashSet<String> actualRelations = new HashSet<>();
            try {
              actualHref.add(jsonObj.getString("href"));
              actualRelations.add(jsonObj.getString("rel"));
            } catch (NullPointerException e) {
              testContext.failNow(e.getMessage());
            }
            HashMap<String, HashSet<String>> actual = new HashMap<>();
            actual.put("href", actualHref);
            actual.put("rel", actualRelations);
            assertEquals(expected, actual);
            testContext.completeNow();
          }
        });

    // check for 400 bad request
  }

  @Test
  @DisplayName("/conformance api test")
  void testConformanceApi(VertxTestContext testContext, WebClient webClient){
    // check for 200 status
    // conformsTo (required)
    // result should be json array of strings
    /*
    * /conformance only requires to have the key conformsTo in the response which is not empty
    */

    testRequest(webClient, HttpMethod.GET, "/conformance")
        .with(requestHeader("Authorization", "Bearer token-string"))
        .expect(statusCode(200))
        .expect(response -> {
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("conformsTo"));
          assertFalse(responseBody.getJsonArray("conformsTo").isEmpty());
        });

    // check for 400 bad request
  }

  @Test
  @DisplayName("/collections api test")
  void testCollectionsApi(VertxTestContext testContext, WebClient webClient){
    // check for 200 status
    // links (required) to self
    // all links contains "rel" and "type"
    // at least one collectionId should be present in the links
    // collections (required)
    // (if available) extent {contains "spatial" and/or "temporal"}
    // spatial contains b-box
    // temporal has interval
    testRequest(webClient, HttpMethod.GET, "/collections")
        .with(requestHeader("Authorization", "Bearer token-string"))
        .expect(statusCode(200))
        .expect(response -> {
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("links"));
          assertTrue(responseBody.getJsonObject("links").containsKey("href")
              && responseBody.getJsonObject("links").containsKey("rel"));
          assertTrue(responseBody.containsKey("collections"));
          assertTrue(responseBody.getJsonArray("collections").getJsonObject(0).containsKey("id")
              && responseBody.getJsonArray("collections").getJsonObject(0).containsKey("link"));
          assertEquals("district_hq", responseBody.getJsonArray("collections")
              .getJsonObject(0).getString("title"));
        });


    // check for 400 bad request
  }
  @Test
  @DisplayName("/collection/:collectionId api test")
  void testCollectionApi(VertxTestContext testContext, WebClient webClient){
    // check for 200 status
    testRequest(webClient, HttpMethod.GET, "/collections/district_hq")
        .with(requestHeader("Authorization", "Bearer token-string"))
        .expect(statusCode(200))
        .expect(response -> {
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("links"));
          assertTrue(responseBody.getJsonArray("links").getJsonObject(0).containsKey("href")
              && responseBody.getJsonArray("links").getJsonObject(0).containsKey("rel"));
          assertTrue(responseBody.containsKey("collections"));
          assertTrue(responseBody.getJsonArray("collections").getJsonObject(0).containsKey("id")
              && responseBody.getJsonArray("collections").getJsonObject(0).containsKey("links"));
          assertEquals("district_hq", responseBody.getJsonArray("collections").getString(0));
        });

    // check for 400 bad request
  }
}
