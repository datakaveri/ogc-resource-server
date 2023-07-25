package ogc.rs.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import static ogc.rs.apiserver.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

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

        Set<String> allowedHeaders = Set.of(HEADER_TOKEN, HEADER_CONTENT_LENGTH, HEADER_CONTENT_TYPE, HEADER_HOST
            ,HEADER_ORIGIN, HEADER_REFERER, HEADER_ACCEPT, HEADER_ALLOW_ORIGIN);

        Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.OPTIONS);

        /* Get base paths from config */
        dxApiBasePath = config().getString("dxApiBasePath");
        Future<RouterBuilder> routerBuilderFut = RouterBuilder.create(vertx, "docs/IUDX-OGC-RS-V0.0.1.yaml");
        routerBuilderFut.compose(routerBuilder -> {

            LOGGER.debug("Info: Mounting routes from OpenApi3 spec");

            RouterBuilderOptions factoryOptions =
                    new RouterBuilderOptions().setMountResponseContentTypeHandler(true);

            routerBuilder.rootHandler(CorsHandler.create("*").allowedHeaders(allowedHeaders)
                    .allowedMethods(allowedMethods));
            routerBuilder.rootHandler(BodyHandler.create(false));
            router = routerBuilder.createRouter();
            return null;
        });

        // TODO: ssl configuration
        HttpServerOptions serverOptions = new HttpServerOptions();
        serverOptions.setCompressionSupported(true).setCompressionLevel(5);
        int port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
        LOGGER.info("Info: Starting HTTP server at port " + port);
        HttpServer server = vertx.createHttpServer(serverOptions);
        server.requestHandler(router).listen(port);
    }
    private void putCommonResponseHeaders() {
        router.route().handler(requestHandler -> {
            requestHandler
                    .response()
                    .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                    .putHeader("Pragma", "no-cache")
                    .putHeader("Expires", "0")
                    .putHeader("X-Content-Type-Options", "nosniff");
            requestHandler.next();
        });
    }

}
