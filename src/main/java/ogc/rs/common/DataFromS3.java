package ogc.rs.common;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.common.awss3.AWS4SignerBase;
import ogc.rs.common.awss3.AWS4SignerForAuthorizationHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DataFromS3 {

  private static final Logger LOGGER = LogManager.getLogger(DataFromS3.class);
  private static HttpClient client;

  private String s3Url;
  static String S3_BUCKET;
  static String S3_REGION;
  static String S3_ACCESS_KEY;
  static String S3_SECRET_KEY;
  private final Map<String, String> headers;
  private URL url;

  public DataFromS3(HttpClient client, String bucket, String region, String accessKey, String secretKey) {
    S3_BUCKET = bucket;
    S3_REGION = region;
    S3_ACCESS_KEY = accessKey;
    S3_SECRET_KEY = secretKey;
    this.s3Url = "https://" + S3_BUCKET + ".s3." + S3_REGION + ".amazonaws.com" + "/";
    DataFromS3.client = client;
    this.headers = new HashMap<>();
  }
  public Future<HttpClientResponse> getDataFromS3(HttpMethod httpMethod) {
    Promise <HttpClientResponse> response  = Promise.promise();
    client.request(httpMethod, url.getDefaultPort(), url.getHost(), url.getPath())
        .compose(req -> {
          headers.forEach(req::putHeader);
          return req.send();
        })
        .onSuccess(res -> {
          if (res.statusCode() == 404) {
            response.fail(new OgcException(404, "Not Found", "File not found."));
          } else if (res.statusCode() == 200) {
            response.complete(res);
          } else {
            LOGGER.error("Internal Server Error, Something went wrong here. {}",res.statusCode());
            response.fail(new OgcException(500, "Internal Server Error", "Internal Server Error"));
          }
        }).onFailure( 
            handler -> { 
              LOGGER.error("Something went wrong when interacting with S3 - {}. Method {}, URL {} ",
                  handler.getMessage(), httpMethod, url.toString());
              
              response.fail( 
                  new OgcException(500, "Internal Server Error", "Internal Server Error")); 
            });
    
    return response.future();
  }

  public void setSignatureHeader (HttpMethod httpMethod) {
    headers.put("x-amz-content-sha256", AWS4SignerBase.EMPTY_BODY_SHA256);
    AWS4SignerForAuthorizationHeader signer =
        new AWS4SignerForAuthorizationHeader(url, httpMethod.name(), "s3", S3_REGION);
    String signedAuthorizationHeader = signer.computeSignature(headers, null, // no query parameters
        AWS4SignerBase.EMPTY_BODY_SHA256, S3_ACCESS_KEY, S3_SECRET_KEY);

    headers.put("Authorization", signedAuthorizationHeader);
  }

  public void setUrlFromString(String strUrl) throws OgcException {
    try {
      this.url = new URL(strUrl);
    } catch (MalformedURLException e) {
      LOGGER.error("Internal Server Error, {}", "Malformed URL");
      throw new OgcException(500, "Internal Server Error", "Internal Server Error");
    }
  }
  public String getFullyQualifiedUrlString (String urlString) {
    this.s3Url = this.s3Url + urlString;
    return s3Url;
  }
}
