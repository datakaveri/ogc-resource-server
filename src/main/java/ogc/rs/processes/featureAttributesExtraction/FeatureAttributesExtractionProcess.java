package ogc.rs.processes.featureAttributesExtraction;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.OgcException;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ogc.rs.processes.featureAttributesExtraction.Constants.*;

/**
 * FeatureAttributesExtractionProcess extracts feature attributes for all features
 * from a given collection. It returns only the specified properties/attributes without geometry data.
 */
public class FeatureAttributesExtractionProcess implements ProcessService {

    private static final Logger LOGGER = LogManager.getLogger(FeatureAttributesExtractionProcess.class);
    private final UtilClass utilClass;
    private final PgPool pgPool;

    public FeatureAttributesExtractionProcess(PgPool pgPool) {
        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
    }

    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        LOGGER.info("Starting Feature Attributes Extraction Process");

        requestInput.put("collectionId", requestInput.getString("collectionId"));
        requestInput.put("attributes", requestInput.getJsonArray("attributes"));

        requestInput.put("progress", calculateProgress(1));
        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_FEATURE_EXTRACTION_MESSAGE)
                .compose(progressHandler -> validateCollectionExists(requestInput))
                .compose(validationHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2))
                                .put("message", COLLECTION_VALIDATION_SUCCESS_MESSAGE)))
                .compose(progressHandler -> validateAttributes(requestInput))
                .compose(attributeHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3))
                                .put("message", "Attributes validated successfully")))
                .compose(progressHandler -> extractFeatureAttributes(requestInput))
                .compose(extractionHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4))
                                .put("message", FEATURE_EXTRACTION_SUCCESS_MESSAGE)))
                .compose(progressHandler -> utilClass.updateJobTableStatus(
                        requestInput, Status.SUCCESSFUL, PROCESS_COMPLETION_MESSAGE))
                .onSuccess(successHandler -> {
                    JsonObject result = requestInput.getJsonObject("extractionResult");
                    LOGGER.info("Feature Attributes Extraction Process completed successfully");
                    promise.complete(result);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error("Feature Attributes Extraction Process failed: {}", failureHandler.getMessage());
                    handleFailure(requestInput, failureHandler, promise);
                });

        return promise.future();
    }

    /**
     * Validates if the specified collection exists in the database
     */
    private Future<Void> validateCollectionExists(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();

        String collectionId = requestInput.getString("collectionId");
        String validationQuery = "SELECT EXISTS(SELECT 1 FROM collections_details WHERE id = $1::uuid)";

        pgPool.withConnection(conn ->
                conn.preparedQuery(validationQuery)
                        .execute(Tuple.of(UUID.fromString(collectionId)))
        ).onSuccess(result -> {
            if (result.iterator().hasNext()) {
                Row row = result.iterator().next();
                boolean exists = row.getBoolean(0);
                if (exists) {
                    LOGGER.debug("Collection {} exists", collectionId);
                    promise.complete();
                } else {
                    LOGGER.error("Collection {} does not exist", collectionId);
                    promise.fail(new OgcException(404, "Not Found", COLLECTION_NOT_FOUND_MESSAGE));
                }
            } else {
                promise.fail(new OgcException(500, "Internal Server Error", "Unable to validate collection existence"));
            }
        }).onFailure(error -> {
            LOGGER.error("Error validating collection existence: {}", error.getMessage());
            promise.fail(new OgcException(500, "Internal Server Error", "Database error during collection validation"));
        });

        return promise.future();
    }

    /**
     * Validates if the specified attributes exist in the collection
     */
    private Future<Void> validateAttributes(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();

        String collectionId = requestInput.getString("collectionId");
        JsonArray attributes = requestInput.getJsonArray("attributes");

        if (attributes == null || attributes.isEmpty()) {
            promise.fail(new OgcException(400, "Bad Request", "Attributes array cannot be empty"));
            return promise.future();
        }

        // Filter out 'geom' and 'id' from attributes for validation
        List<String> attributesToValidate = attributes.stream()
                .map(Object::toString)
                .filter(attr -> !attr.equals("geom") && !attr.equals("id"))
                .collect(Collectors.toList());

        if (attributesToValidate.isEmpty()) {
            // If only geom/id were specified, no need to validate
            promise.complete();
            return promise.future();
        }

        // Build query to check if attributes exist as columns
        String validateAttributesQuery = String.format(
                "SELECT column_name FROM information_schema.columns WHERE table_name = '%s' AND column_name = ANY($1)",
                collectionId
        );

        pgPool.withConnection(conn ->
                conn.preparedQuery(validateAttributesQuery)
                        .execute(Tuple.of(attributesToValidate.toArray(new String[0])))
        ).onSuccess(result -> {
            List<String> existingAttributes = new ArrayList<>();
            for (Row row : result) {
                existingAttributes.add(row.getString(0)); // Get by column index
            }

            List<String> missingAttributes = attributesToValidate.stream()
                    .filter(attr -> !existingAttributes.contains(attr))
                    .collect(Collectors.toList());

            if (!missingAttributes.isEmpty()) {
                LOGGER.error("The following attributes do not exist in the collection: {}", missingAttributes);
                promise.fail(new OgcException(400, "Bad Request", ATTRIBUTES_NOT_FOUND_MESSAGE));
            } else {
                promise.complete();
            }
        }).onFailure(error -> {
            LOGGER.error("Error validating attributes: {}", error.getMessage());
            promise.fail(new OgcException(500, "Internal Server Error", "Database error during attribute validation"));
        });

        return promise.future();
    }

    /**
     * Extracts feature attributes for all features in the collection
     */
    private Future<JsonObject> extractFeatureAttributes(JsonObject requestInput) {
        Promise<JsonObject> promise = Promise.promise();

        String collectionId = requestInput.getString("collectionId");
        JsonArray attributesArray = requestInput.getJsonArray("attributes");

        List<String> requestedAttributes = attributesArray.stream()
                .map(Object::toString)
                .filter(attr -> !attr.equals("geom"))
                .collect(Collectors.toList());

        if (!requestedAttributes.contains("id")) {
            requestedAttributes.add(0, "id");
        }

        String selectClause = requestedAttributes.stream()
                .map(attr -> String.format("\"%s\"", attr))
                .collect(Collectors.joining(", "));

        String extractionQuery = String.format(
                "SELECT %s FROM \"%s\" ORDER BY id",
                selectClause, collectionId
        );

        LOGGER.debug("Extraction query: {}", extractionQuery);

        pgPool.withConnection(conn ->
                conn.preparedQuery(extractionQuery)
                        .execute()
        ).onSuccess(result -> {
            JsonArray features = new JsonArray();

            for (Row row : result) {
                JsonObject properties = new JsonObject();
                Integer idValue = null;

                for (int i = 0; i < requestedAttributes.size(); i++) {
                    String attr = requestedAttributes.get(i);
                    Object value = row.getValue(i);

                    if ("id".equals(attr)) {
                        idValue = (Integer) value;
                    } else if (value != null) {
                        properties.put(attr, value);
                    }
                }

                JsonObject feature = new JsonObject()
                        .put("id", idValue)
                        .put("properties", properties);

                features.add(feature);
            }

            JsonObject extractionResult = new JsonObject()
                    .put("features", features);

            requestInput.put("extractionResult", extractionResult);

            LOGGER.debug("Successfully extracted {} features", features.size());
            promise.complete(extractionResult);

        }).onFailure(error -> {
            LOGGER.error("Error extracting feature attributes: {}", error.getMessage());
            promise.fail(new OgcException(500, "Internal Server Error", FEATURE_ATTRIBUTES_EXTRACTION_FAILURE_MESSAGE));
        });

        return promise.future();
    }

    /**
     * Handles process failures by updating job status and propagating the error
     */
    private void handleFailure(JsonObject requestInput, Throwable failureHandler, Promise<JsonObject> promise) {
        String errorMessage = failureHandler.getMessage();

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Feature Attributes Extraction Process failed due to: {}", errorMessage);
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
        // 5 total steps: start, collection validation, attribute validation, extraction, completion
        return (step * 100) / 5;
    }
}