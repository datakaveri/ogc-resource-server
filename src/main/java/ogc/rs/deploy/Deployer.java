package ogc.rs.deploy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import ogc.rs.apiserver.ApiServerVerticle;
import ogc.rs.apiserver.router.RouterManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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

        if(System.getProperty("s3.mock") != null){
            LOGGER.fatal("SSL checks when connecting to S3 are DISABLED using system property 's3.mock'");
        }

        EventBusOptions ebOptions = new EventBusOptions();
        VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions);

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
        recursiveDeploy(vertx, configuration, 0);
    }

    private static JsonObject getConfigForModule(int moduleIndex, JsonObject configurations) {
        JsonObject commonConfigs=configurations.getJsonObject("commonConfig");
        JsonObject config = configurations.getJsonArray("modules").getJsonObject(moduleIndex);
        return config.mergeIn(commonConfigs, true);
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
