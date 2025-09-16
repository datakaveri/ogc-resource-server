package ogc.rs.processes.echo;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ogc.rs.processes.echo.Constants.*;

/**
 * EchoProcess is a simple process that echoes back the provided name and optional message
 * in the specified MCF (Machine-readable Content Format) structure.
 */
public class EchoProcess implements ProcessService {

    private static final Logger LOGGER = LogManager.getLogger(EchoProcess.class);
    private final UtilClass utilClass;

    public EchoProcess(Pool pgPool) {
        this.utilClass = new UtilClass(pgPool);
    }

    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        LOGGER.info("Starting Echo Process");

        // Extract input parameters
        String name = requestInput.getString("name");
        String message = requestInput.getString("message");

        // Update job status to running
        requestInput.put("progress", calculateProgress(1));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_ECHO_MESSAGE)
                .compose(progressHandler -> processEcho(requestInput, name, message))
                .compose(echoHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2))
                                .put("message", ECHO_SUCCESS_MESSAGE)))
                .compose(progressHandler -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, PROCESS_COMPLETION_MESSAGE))
                .onSuccess(successHandler -> {
                    JsonObject result = requestInput.getJsonObject("echoResult");
                    LOGGER.info("Echo Process completed successfully");
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(ECHO_PROCESSING_FAILURE_MESSAGE);
                    handleFailure(requestInput, failureHandler, promise);
                });

        return promise.future();
    }

    /**
     * Processes the echo operation by creating the MCF response structure
     */
    private Future<JsonObject> processEcho(JsonObject requestInput, String name, String message) {
        Promise<JsonObject> promise = Promise.promise();

        try {
            // Create the echo content
            JsonObject echoContent = new JsonObject();
            echoContent.put("name", name);

            if (message != null && !message.trim().isEmpty()) {
                echoContent.put("message", message);
            }

            // Create the MCF (Machine-readable Content Format) structure
            JsonObject mcfResult = new JsonObject()
                    .put("type", "object")
                    .put("contentMediaType", "application/json")
                    .put("data", echoContent);

            // Create the final result structure as specified in output
            JsonObject result = new JsonObject()
                    .put("result", mcfResult);

            // Store result in request input for later use
            requestInput.put("echoResult", result);

            LOGGER.debug("Echo processed successfully for name: {}", name);
            promise.complete(result);

        } catch (Exception e) {
            LOGGER.error("Error processing echo: {}", e.getMessage());
            promise.fail(e);
        }

        return promise.future();
    }

    /**
     * Handles process failures by updating job status and propagating the error
     */
    private void handleFailure(JsonObject requestInput, Throwable failureHandler, Promise<JsonObject> promise) {
        String errorMessage = failureHandler.getMessage();

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Echo Process failed due to: {}", errorMessage);
                    promise.fail(failureHandler);
                })
                .onFailure(jobStatusFailureHandler -> {
                    LOGGER.error("Failed to update job status: {}", jobStatusFailureHandler.getMessage());
                    promise.fail(jobStatusFailureHandler);
                });
    }

    /**
     * Calculates progress percentage based on step number
     */
    private int calculateProgress(int step) {
        // 3 total steps: start, process echo, completion
        return (step * 100) / 3;
    }
}