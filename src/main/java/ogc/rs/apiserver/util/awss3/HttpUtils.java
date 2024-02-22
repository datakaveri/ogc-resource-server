package ogc.rs.apiserver.util.awss3;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * <b>CODE COPIED FROM <a href=
 * "https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-examples-using-sdks.html#sig-v4-examples-using-sdk-java">https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-examples-using-sdks.html#sig-v4-examples-using-sdk-java</a></b>.</br>
 * </br>
 * Various Http helper routines
 */
public class HttpUtils {
  public static String urlEncode(String url, boolean keepPathSlash) {
    String encoded;
    try {
      encoded = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 encoding is not supported.", e);
    }
    if (keepPathSlash) {
      encoded = encoded.replace("%2F", "/");
    }
    return encoded;
  }
}
