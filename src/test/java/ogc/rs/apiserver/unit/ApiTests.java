package ogc.rs.apiserver.unit;

import io.reactiverse.junit5.web.WebClientOptionsInject;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import ogc.rs.database.DatabaseService;
import ogc.rs.database.DatabaseServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static io.reactiverse.junit5.web.TestRequest.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class})
public class ApiTests {
  // move test port to the Constants file
  private static final int OGC_TEST_PORT = 50000;
  private static Vertx vertxObj;
  static JsonObject config;

  static DatabaseService dbService = Mockito.mock(DatabaseServiceImpl.class);

  @WebClientOptionsInject
  public WebClientOptions options =
          new WebClientOptions().setDefaultHost("localhost").setDefaultPort(OGC_TEST_PORT).setVerifyHost(true);

  static Logger LOGGER = LogManager.getLogger(ApiTests.class);
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
    vertxObj.deployVerticle(config.getJsonObject("apiserver").getString("id")
        , new DeploymentOptions()
            .setInstances(config.getJsonObject("apiserver").getInteger("verticleInstances"))
            .setConfig(config.getJsonObject("apiserver"))
        , deploy -> {
            if(deploy.succeeded()){
              LOGGER.info("Deployed verticle- {}", config.getJsonObject("apiserver").getString("id"));
              vertxObj.setTimer(2000, fire -> testContext.completeNow()
              );
            }
            else {
              LOGGER.fatal("Failed to deploy verticle- {}", deploy.cause());
              testContext.failNow(deploy.cause());
            }
        } );
  }

  @AfterAll
  public static void finish(Vertx vertx, VertxTestContext testContext) {
    vertx.close().onSuccess(success -> testContext.completeNow())
        .onFailure(failure -> testContext.failNow(failure.getMessage()));
  }
  @Test
  @DisplayName("/ (landing page) api test")
  void testLandingPage(Vertx vertx, VertxTestContext testContext, WebClient webClient){
    testRequest(webClient, HttpMethod.GET, "/")
        .with(requestHeader("Authorization", "Bearer token-string"))
        .expect(statusCode(200))
        .expect(response -> {
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("links"));
          assertFalse(responseBody.getJsonArray("links").isEmpty());
          HashMap<String, HashSet<String>> expected = new HashMap<>();
          HashSet<String> expectedHref = new HashSet<>();
          expectedHref.add("https://ogc.iudx.io/");
          expectedHref.add("https://ogc.iudx.io/conformance");
          expectedHref.add("https://ogc.iudx.io/api");
          expectedHref.add("https://ogc.iudx.io/collections");

          HashSet<String> expectedRelations = new HashSet<>();
          expectedRelations.add("service-desc");
          expectedRelations.add("conformance");
          expectedRelations.add("self");
          expectedRelations.add("data");

          expected.put("href", expectedHref);
          expected.put("rel", expectedRelations);
          HashMap<String, HashSet<String>> actual = new HashMap<>();
          HashSet<String> actualHref = new HashSet<>();
          HashSet<String> actualRelations = new HashSet<>();
          // TODO: use Java8 stream API
          for (Object obj: responseBody.getJsonArray("links")){
            JsonObject jsonObj = (JsonObject) obj;
            assertFalse(jsonObj.isEmpty());
            assertTrue(jsonObj.containsKey("href") && jsonObj.containsKey("rel"));

            try {
//              LOGGER.debug("href-[{}], rel-[{}]", jsonObj.getString("href"), jsonObj.getString("rel"));
              actualHref.add(jsonObj.getString("href"));
              actualRelations.add(jsonObj.getString("rel"));
            } catch (NullPointerException e) {
              testContext.failNow(e.getMessage());
            }
          }
          actual.put("href", actualHref);
          actual.put("rel", actualRelations);
          assertEquals(expected, actual);
        }).send(testContext);

    // check for 400 bad request
  }

  @Test
  @DisplayName("/conformance api test")
  void testConformanceApi(VertxTestContext testContext, WebClient webClient){
    testRequest(webClient, HttpMethod.GET, "/conformance")
        .expect(statusCode(200))
        .expect(response -> {
          LOGGER.debug("I'm here, where are you?");
          JsonObject responseBody = response.bodyAsJsonObject();
          assertTrue(responseBody.containsKey("conformsTo"));
          assertFalse(responseBody.getJsonArray("conformsTo").isEmpty());
        }).send(testContext);
  }

//  @Test
//  @DisplayName("/collections api test")
//  void testCollectionsApi(VertxTestContext testContext, WebClient webClient){
//    // check for 200 status
//    // links (required) to self
//    // all links contains "rel" and "type"
//    // at least one collectionId should be present in the links
//    // collections (required)
//    // (if available) extent {contains "spatial" and/or "temporal"}
//    // spatial contains b-box
//    // temporal has interval
//
//    List<JsonObject> collection = new ArrayList<>();
//    collection.add(new JsonObject()
//        .put("id", "some-uuid")
//        .put("itemType", "feature")
//        .put("crs", new JsonArray()
//            .add("some-crs"))
//        .put("links", new JsonArray()
//            .add(new JsonObject()
//                .put("href","some-link")
//                .put("rel","self")
//                .put("title","some-title")
//                .put("description","some-desc"))));
//
//    Mockito.doAnswer(ans -> Future.succeededFuture(collection)).when(dbService).getCollections();
//
//    testRequest(webClient, HttpMethod.GET, "/collections")
//        .expect(statusCode(200))
//        .expect(response -> {
//          JsonObject responseBody = response.bodyAsJsonObject();
//          assertTrue(responseBody.containsKey("links"));
//          assertTrue(responseBody.getJsonObject("links").containsKey("href")
//              && responseBody.getJsonObject("links").containsKey("rel"));
//          assertTrue(responseBody.containsKey("collections"));
//          assertTrue(responseBody.getJsonArray("collections").getJsonObject(0).containsKey("id")
//              && responseBody.getJsonArray("collections").getJsonObject(0).containsKey("href"));
//          assertEquals("some-title", responseBody.getJsonArray("collections")
//              .getJsonObject(0).getString("title"));
//        }).send(testContext);


    // check for 400 bad request
  //}
//  @Test
//  @DisplayName("/collection/:collectionId api test")
//  void testCollectionApi(VertxTestContext testContext, WebClient webClient){
//    // check for 200 status
//    List<JsonObject> collection = new ArrayList<>();
//    collection.add(new JsonObject()
//        .put("id", "district_hq")
//        .put("itemType", "feature")
//        .put("crs", new JsonArray()
//            .add("some-crs"))
//        .put("links", new JsonArray()
//            .add(new JsonObject()
//                .put("href","some-link")
//                .put("rel","self")
//                .put("title","some-title")
//                .put("description","some-desc"))));
//
//    // Mockito.doAnswer(ans -> Future.succeededFuture(collection)).when(dbService).getCollection("district_hq");
//    Mockito.when(dbService.getCollection(Mockito.anyString()))
//        .thenReturn(Future.succeededFuture(collection));
//
//    testRequest(webClient, HttpMethod.GET, "/collections/district_hq")
//        .expect(statusCode(200))
//        .expect(response -> {
//          JsonObject responseBody = response.bodyAsJsonObject();
//          assertTrue(responseBody.containsKey("links"));
//          assertTrue(responseBody.getJsonArray("links").getJsonObject(0).containsKey("href")
//              && responseBody.getJsonArray("links").getJsonObject(0).containsKey("rel"));
//          assertTrue(responseBody.containsKey("collections"));
//          assertTrue(responseBody.getJsonArray("collections").getJsonObject(0).containsKey("id")
//              && responseBody.getJsonArray("collections").getJsonObject(0).containsKey("links"));
//          assertEquals("district_hq", responseBody.getJsonArray("collections").getString(0));
//        }).send(testContext);
//
//    // check for 400 bad request
//  }

  // add features and feature api tests
  @Test
  @DisplayName("Display Features for a collection")
  void displayFeatures (VertxTestContext testContext){
    testContext.completeNow();
  }

  @Test
  @DisplayName("Display Features for a collection with additional filter parameters")
  void displayFeaturesWithFilters (VertxTestContext testContext){
    // use checkpoints
    testContext.completeNow();
  }

  @Test
  @DisplayName("Display features for a feature Id for a collection")
  void displayFeaturesWithId (VertxTestContext testContext){
    testContext.completeNow();
  }

  @Test
  @DisplayName("Bad request for features")
  void badRequestFeatures (VertxTestContext testContext){
    testContext.completeNow();
  }



  //feature api test- 200 and 400, bad request will have all malformed parameters for limit, bbox, datetime

}
