package ogc.rs.restAssuredTest;

import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.restassured.RestAssured.*;

public class RestAssuredConfigExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    String testHost = System.getProperty("intTestHost");

    if (testHost != null) {
      RestAssured.baseURI = "http://" + testHost;
    } else {
      RestAssured.baseURI = "http://localhost";
    }

    String testPort = System.getProperty("intTestPort");

    if (testPort != null) {
      RestAssured.port = Integer.parseInt(testPort);
    } else {
      RestAssured.port = 8443;
    }

    String proxyHost = System.getProperty("intTestProxyHost");
    String proxyPort = System.getProperty("intTestProxyPort");

    if (proxyHost != null && proxyPort != null) {
      proxy(proxyHost, Integer.parseInt(proxyPort));
    }

    enableLoggingOfRequestAndResponseIfValidationFails();
  }
}
