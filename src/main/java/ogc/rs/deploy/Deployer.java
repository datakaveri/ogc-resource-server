package ogc.rs.deploy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.router.RouterManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Deployer {
    private static final Logger LOGGER = LogManager.getLogger(Deployer.class);

    public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
        if (i >= configs.getJsonArray("modules").size()) {
            LOGGER.info("Deployed all");
            return;
        }
        JsonObject moduleConfigurations = getConfigForModule(i, configs);
        moduleConfigurations.put("host", configs.getString("host"));
        String moduleName = moduleConfigurations.getString("id");
        int numInstances = moduleConfigurations.getInteger("verticleInstances");

        Promise<String> deployed = Promise.promise();

        if (moduleName.contains("ApiServerVerticle")) {
          RouterManager routerMgr = new RouterManager(vertx, moduleConfigurations);

          vertx.deployVerticle(() -> {
            ApiServerVerticle api = new ApiServerVerticle();
            routerMgr.registerApiServer(api);
            return api;
          }, new DeploymentOptions().setInstances(numInstances).setConfig(moduleConfigurations),
              deployed);
        } else {

          vertx.deployVerticle(moduleName,
              new DeploymentOptions().setInstances(numInstances).setConfig(moduleConfigurations),
              deployed);
        }

        deployed.future().onComplete(ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("Deployed " + moduleName);
                        recursiveDeploy(vertx, configs, i + 1);
                    } else {
                        LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
                    }
                });
    }

    public static void deploy(String configPath) {
        if(System.getProperty("disable.auth") != null){
            LOGGER.fatal("Token Authentication and Authorization is DISABLED using system property 'disable.auth'");
        }

        EventBusOptions ebOptions = new EventBusOptions();
        VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions).setMaxWorkerExecuteTimeUnit(TimeUnit.HOURS).setMaxWorkerExecuteTime(1).setMetricsOptions(getMetricsOptions());  // Enable Micrometer metrics

        LOGGER.debug("metrics-options" + options.getMetricsOptions());
        String config;
        try {
            config = Files.readString(Paths.get(configPath));
        } catch (Exception e) {
            LOGGER.fatal("Couldn't read configuration file");
            return;
        }
        if (config.length() < 1) {
            LOGGER.fatal("Couldn't read configuration file");
            return;
        }
        JsonObject configuration = new JsonObject(config);
        Vertx vertx = Vertx.vertx(options);

        setJvmMetrics();  // Bind JVM metrics to registry

        recursiveDeploy(vertx, configuration, 0);
    }

    private static JsonObject getConfigForModule(int moduleIndex, JsonObject configurations) {
        JsonObject commonConfigs=configurations.getJsonObject("commonConfig");
        JsonObject config = configurations.getJsonArray("modules").getJsonObject(moduleIndex);
        return config.mergeIn(commonConfigs, true);
    }

    /**
     * Returns an instance of {@link MetricsOptions} configured with Micrometer
     * metrics options for Prometheus along with additional labels and enabled status.
     * @return an instance of {@link MetricsOptions}
     */
    public static MetricsOptions getMetricsOptions() {
        return new MicrometerMetricsOptions()
                .setPrometheusOptions(
                        new VertxPrometheusOptions().setEnabled(true).setStartEmbeddedServer(true)
                                .setEmbeddedServerOptions(new io.vertx.core.http.HttpServerOptions().setPort(9000)))
                .setEnabled(true);
    }

    /**
     * Binds JVM metrics to the default meter registry.
     * The registry collects and records various JVM-related metrics such as memory usage,
     * garbage collection statistics, and thread count.
     */
    public static void setJvmMetrics() {
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if (registry != null) {
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
        } else {
            LOGGER.warn("MeterRegistry is not available, JVM metrics will not be recorded.");
        }
    }

    public static void main(String[] args) {
        CLI cli = CLI.create("IUDX OGC").setSummary("A CLI to deploy the OGC resource server")
                .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
                        .setDescription("display help"))
                .addOption(new Option().setLongName("config").setShortName("c")
                        .setRequired(true).setDescription("configuration file"));

        StringBuilder usageString = new StringBuilder();
        cli.usage(usageString);
        CommandLine commandLine = cli.parse(Arrays.asList(args), false);
        if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
            String configPath = commandLine.getOptionValue("config");
            deploy(configPath);
        } else {
            LOGGER.info(usageString);
        }
    }
}
