package iudx.ogc.rs.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static iudx.ogc.rs.apiserver.util.Constants.*;

/**
 * The OGC Resource Server API Verticle.
 *
 * <h1>OGC Resource Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the OGC Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2023-06-12
 */
public class ApiServerVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
    private Router router;
    private String dxApiBasePath;


    /**
     * This method is used to start the Verticle. It deploys a verticle in a cluster/single instance, reads the
     * configuration, obtains a proxy for the Event bus services exposed through service discovery,
     * start an HTTPs server at port 8443 or an HTTP server at port 8080.
     *
     * @throws Exception which is a startup exception TODO Need to add documentation for all the
     */
    @Override
    public void start() throws Exception {

        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add(HEADER_ACCEPT);
        allowedHeaders.add(HEADER_TOKEN);
        allowedHeaders.add(HEADER_CONTENT_LENGTH);
        allowedHeaders.add(HEADER_CONTENT_TYPE);
        allowedHeaders.add(HEADER_HOST);
        allowedHeaders.add(HEADER_ORIGIN);
        allowedHeaders.add(HEADER_REFERER);
        allowedHeaders.add(HEADER_ALLOW_ORIGIN);
        allowedHeaders.add(HEADER_PUBLIC_KEY);
        allowedHeaders.add(HEADER_RESPONSE_FILE_FORMAT);
        allowedHeaders.add(HEADER_OPTIONS);

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.OPTIONS);

        router = Router.router(vertx);

        /* Get base paths from config */
        dxApiBasePath = config().getString("dxApiBasePath");
    }
}
