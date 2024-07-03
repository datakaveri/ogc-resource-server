package ogc.rs.processes.collectionAppending;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import ogc.rs.apiserver.util.ProcessException;
import ogc.rs.common.DataFromS3;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.collectionOnboarding.CollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static ogc.rs.processes.collectionAppending.Constants.*;


/**
 * This class handles the process of appending a collection of GeoJSON data (or any format of GIS data)
 * to an existing collection in a PostgreSQL database.
 * It includes steps for checking for resource ownership and existing collection ,
 * validating schemas and CRS,appending data to temporary table, merging temporary table into existing collection table ,
 * updating bbox values in collection table and deleting temporary table
 */

public class CollectionAppendingProcess implements ProcessService {

    private static final Logger LOGGER = LogManager.getLogger(CollectionAppendingProcess.class);
    private final PgPool pgPool;
    private final UtilClass utilClass;
    private final CollectionOnboardingProcess collectionOnboarding;
    private String awsEndPoint;
    private String accessKey;
    private String secretKey;
    private String awsBucketUrl;
    private String databaseName;
    private String databaseHost;
    private String databasePassword;
    private String databaseUser;
    private int databasePort;
    private final Vertx vertx;
    private final boolean VERTX_EXECUTE_BLOCKING_IN_ORDER = false;

    /**
     * Constructs a new instance of CollectionAppendingProcess.
     *
     * @param pgPool             The PostgreSQL connection pool used for database operations.
     * @param webClient          The WebClient used for making HTTP requests.
     * @param config             The configuration details as a JsonObject.
     * @param dataFromS3         The DataFromS3 instance for accessing data from AWS S3.
     * @param vertx              The Vert.x instance for executing asynchronous and event-driven tasks.
     */

    public CollectionAppendingProcess(PgPool pgPool, WebClient webClient, JsonObject config, DataFromS3 dataFromS3, Vertx vertx) {

        this.pgPool = pgPool;
        this.utilClass = new UtilClass(pgPool);
        this.collectionOnboarding = new CollectionOnboardingProcess(pgPool, webClient, config, dataFromS3, vertx);
        this.vertx = vertx;
        initializeConfig(config);
    }

    /**
     * Initializes configuration parameters from the provided JsonObject.
     *
     * @param config the configuration containing AWS and database details.
     */

    private void initializeConfig(JsonObject config) {

        this.awsEndPoint = config.getString("awsEndPoint");
        this.accessKey = config.getString("awsAccessKey");
        this.secretKey = config.getString("awsSecretKey");
        this.awsBucketUrl = config.getString("s3BucketUrl");
        this.databaseName = config.getString("databaseName");
        this.databaseHost = config.getString("databaseHost");
        this.databasePassword = config.getString("databasePassword");
        this.databaseUser = config.getString("databaseUser");
        this.databasePort = config.getInteger("databasePort");

    }

    /**
     * Executes a series of asynchronous operations for processing a request input.
     * This method handles a sequence of tasks related to data processing and updates job status and progress
     * accordingly.
     *
     * @param requestInput The input JSON object containing parameters and data necessary for processing.
     *                     It should include a "resourceId" to identify the resource being processed.
     * @return A Future that completes with a JsonObject when all operations are successfully executed,
     *         or fails if any operation encounters an error.
     */

    @Override
    public Future<JsonObject> execute(JsonObject requestInput) {

        Promise<JsonObject> objectPromise = Promise.promise();

        requestInput.put("progress", calculateProgress(1, 7));

        String tableID = requestInput.getString("resourceId");
        requestInput.put("collectionsDetailsTableId", tableID);

        utilClass.updateJobTableStatus(requestInput, Status.RUNNING, STARTING_APPEND_PROCESS_MESSAGE)
                .compose(progressUpdateHandler -> checkIfCollectionPresent(requestInput))
                .compose(collectionCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(2, 7)).put("message", COLLECTION_EXISTS_MESSAGE)))
                .compose(progressUpdateHandler->collectionOnboarding.makeCatApiRequest(requestInput))
                .compose(resourceOwnershipCheckHandler->utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(3, 7)).put("message", RESOURCE_OWNERSHIP_CHECK_MESSAGE)))
                .compose(progressUpdateHandler -> validateSchemaAndCRS(requestInput))
                .compose(schemaCheckHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress", calculateProgress(4, 7)).put("message", SCHEMA_CRS_VALIDATION_SUCCESS_MESSAGE)))
                .compose(progressUpdateHandler -> appendDataToTempTable(requestInput))
                .compose(appendHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(5,7)).put("message",APPEND_PROCESS_MESSAGE)))
                .compose(progressUpdateHandler -> mergeTempTableToCollectionTable(requestInput))
                .compose(mergeHandler -> utilClass.updateJobTableProgress(
                        requestInput.put("progress",calculateProgress(6,7)).put("message",MERGE_TEMP_TABLE_MESSAGE)))
                .compose(progressUpdateHandler->collectionOnboarding.ogr2ogrCmdExtent(requestInput))
                .compose(checkDbHandler -> utilClass.updateJobTableStatus(requestInput, Status.SUCCESSFUL,BBOX_UPDATE_MESSAGE))
                .onSuccess(successHandler -> {
                    deleteTempTable(requestInput)
                            .onComplete(deleteHandler ->
                                    LOGGER.debug(APPEND_SUCCESS_MESSAGE)
                            );
                    objectPromise.complete();
                }).onFailure(failureHandler ->
                        deleteTempTable(requestInput)
                                .onComplete(deleteHandler ->{
                                    handleFailure(requestInput, failureHandler.getMessage(), objectPromise);
                                    LOGGER.error(APPEND_FAILURE_MESSAGE + failureHandler.getMessage());
                                })
                );

        return objectPromise.future();

    }

    /**
     * Checks if a collection identified by "collectionsDetailsTableId" exists in the database.
     * Resolves the Promise if the collection is found, or fails the Promise if not found or an error occurs.
     *
     * @param requestInput The JsonObject containing the input parameters, including "collectionsDetailsTableId"
     *                     to identify the collection to be checked.
     * @return A Future<Void> that completes successfully if the collection exists,
     *         or fails if the collection does not exist or an error occurs during the database query.
     */

    private Future<Void> checkIfCollectionPresent(JsonObject requestInput) {

        Promise<Void> promise = Promise.promise();
        pgPool.withConnection(
                sqlConnection -> sqlConnection.preparedQuery(COLLECTIONS_DETAILS_SELECT_QUERY)
                        .execute(Tuple.of(requestInput.getString("collectionsDetailsTableId")))
                        .onSuccess(successHandler -> {
                            if (successHandler.size() > 0) {
                                promise.complete();
                            } else {
                                LOGGER.error(COLLECTION_NOT_FOUND_MESSAGE + ":" + requestInput.getString("collectionsDetailsTableId"));
                                promise.fail(COLLECTION_NOT_FOUND_MESSAGE);
                            }
                        }).onFailure(failureHandler -> {
                            LOGGER.error(COLLECTION_EXISTENCE_FAIL_CHECK +  ":" + requestInput.getString("collectionsDetailsTableId") + failureHandler.getMessage());
                            promise.fail(COLLECTION_EXISTENCE_FAIL_CHECK);
                        }));

        return promise.future();

    }

    /**
     * Validates the schema and Coordinate Reference System (CRS) information of a GeoJSON dataset.
     *
     * This method fetches dataset information using OGRInfo based on the provided input, extracts
     * attributes and CRS details, and validates them against expected values (EPSG authority and SRID 4326).
     * Additionally, it retrieves the database schema and compares it with the extracted attributes.
     * If any validation fails, it logs an error message and fails the Future.
     *
     * @param requestInput the JsonObject containing input parameters required for fetching dataset information
     *                     and validating schema and CRS
     * @return a Future<Void> that completes successfully if the schema and CRS validations pass,
     *         or fails if any validation condition is not met
     */

    private Future<Void> validateSchemaAndCRS(JsonObject requestInput) {
        Promise<Void> promise = Promise.promise();

        vertx.executeBlocking(schemaCRSCheckPromise -> {
                    try {
                        JsonObject geoJsonDataSetInfo = fetchDataSetInfoWithOgrinfo(requestInput);
                        Set<String> geoJsonAttributes = extractAttributesFromDataSetInfo(geoJsonDataSetInfo);
                        Map<String, String> authorityAndCode = extractCRSFromDataSetInfo(geoJsonDataSetInfo);

                        // Add 'geom' and 'id' attributes to the geoJsonAttributes set
                        geoJsonAttributes.add("geom");
                        geoJsonAttributes.add("id");

                        // Check if organisation is "EPSG" and sr_id is "4326"
                        if (!"EPSG".equals(authorityAndCode.get("authority"))) {
                            String errorMsg = INVALID_ORGANISATION_MESSAGE + ": " + authorityAndCode.get("authority");
                            LOGGER.error(errorMsg);
                            schemaCRSCheckPromise.fail(INVALID_ORGANISATION_MESSAGE);
                            return;
                        }
                        LOGGER.debug(VALID_ORGANISATION_MESSAGE);

                        if (!"4326".equals(authorityAndCode.get("code"))) {
                            String errorMsg = INVALID_SR_ID_MESSAGE + ": " + authorityAndCode.get("code");
                            LOGGER.error(errorMsg);
                            schemaCRSCheckPromise.fail(INVALID_SR_ID_MESSAGE);
                            return;
                        }
                        LOGGER.debug(VALID_SR_ID_MESSAGE);

                        getDbSchema(requestInput.getString("collectionsDetailsTableId"))
                                .onSuccess(dbSchema -> {
                                    if (geoJsonAttributes.equals(dbSchema)) {
                                        LOGGER.debug(SCHEMA_VALIDATION_SUCCESS_MESSAGE);
                                        schemaCRSCheckPromise.complete();
                                    } else {
                                        String errorMsg = SCHEMA_VALIDATION_FAILURE_MESSAGE + " GeoJSON schema: " + geoJsonAttributes + ", DB schema: " + dbSchema;
                                        LOGGER.error(errorMsg);
                                        schemaCRSCheckPromise.fail(SCHEMA_VALIDATION_FAILURE_MESSAGE);
                                    }
                                })
                                .onFailure(schemaCRSCheckPromise::fail);
                    } catch (Exception e) {
                        LOGGER.error(SCHEMA_CRS_VALIDATION_FAILURE_MESSAGE +  ":" + e);
                        schemaCRSCheckPromise.fail(SCHEMA_CRS_VALIDATION_FAILURE_MESSAGE);
                    }
                }, VERTX_EXECUTE_BLOCKING_IN_ORDER).onSuccess(handler -> promise.complete())
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Executes ogrinfo command-line tool with the provided input parameters to fetch dataset information.
     *
     * @param input The JsonObject containing parameters needed for ogrinfo command execution.
     * @return A JsonObject representing the dataset information fetched by ogrinfo.
     * @throws IOException If an I/O error occurs during the execution of ogrinfo command.
     */

    private JsonObject fetchDataSetInfoWithOgrinfo(JsonObject input) throws IOException {

        CommandLine cmdLine = getOrgInfoCommandLine(input);
        DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
        executor.setWatchdog(watchdog);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));

        try {
            int exitValue = executor.execute(cmdLine);
            LOGGER.debug("ogrinfo executed with exit value: {} " , exitValue);
            String output = stdout.toString();
            // Removing the initial message if necessary
            String jsonResponse = output.replace("Had to open data source read-only.", "");
            return new JsonObject(Buffer.buffer(jsonResponse));
        } catch (IOException e) {
            LOGGER.error(OGR_INFO_FAILED_MESSAGE + stderr + e);
            throw e;
        }
    }

    /**
     * Constructs a command line for executing ogrinfo tool to retrieve dataset information.
     *
     * @param input The JsonObject containing input parameters required for constructing the command line.
     *              It should at least contain "fileName" which specifies the file name to fetch from AWS S3.
     * @return A CommandLine object configured for ogrinfo tool with necessary arguments and configurations.
     */

    private CommandLine getOrgInfoCommandLine(JsonObject input) {

        LOGGER.debug("Inside ogrinfo command line to get Dataset Information");

        String filename = "/" + input.getString("fileName");
        CommandLine ogrinfo = new CommandLine("ogrinfo");
        ogrinfo.addArgument("--config");
        ogrinfo.addArgument("CPL_VSIL_USE_TEMP_FILE_FOR_RANDOM_WRITE");
        ogrinfo.addArgument("NO");

        setS3OptionsForTesting(ogrinfo);
        ogrinfo.addArgument("--config");
        ogrinfo.addArgument("AWS_S3_ENDPOINT");
        ogrinfo.addArgument(awsEndPoint);
        ogrinfo.addArgument("--config");
        ogrinfo.addArgument("AWS_ACCESS_KEY_ID");
        ogrinfo.addArgument(accessKey);
        ogrinfo.addArgument("--config");
        ogrinfo.addArgument("AWS_SECRET_ACCESS_KEY");

        ogrinfo.addArgument(secretKey);
        ogrinfo.addArgument("-json");
        ogrinfo.addArgument("-ro");
        ogrinfo.addArgument(String.format("/vsis3/%s%s", awsBucketUrl, filename));

        return ogrinfo;
    }

    /**
     * Extracts schema (attribute names) from the given JsonObject representing dataset information.
     *
     * @param geoJsonDataSetInfo The JsonObject containing dataset information, expected to have a "layers" array
     *                           where attribute fields are extracted from the first layer.
     * @return A Set of attribute names extracted from the fields of the first layer in the dataset information.
     *         Returns an empty set if no layers or fields are found.
     */

    private Set<String> extractAttributesFromDataSetInfo(JsonObject geoJsonDataSetInfo) {

        JsonArray layers = geoJsonDataSetInfo.getJsonArray("layers", new JsonArray());
        if (layers.isEmpty()) {
            return Set.of(); // Return empty set if no layers are found
        }
        JsonObject firstLayer = layers.getJsonObject(0);
        JsonArray fields = firstLayer.getJsonArray("fields", new JsonArray());

        return fields.stream()
                .map(field -> ((JsonObject) field).getString("name"))
                .collect(Collectors.toSet());
    }

    /**
     * Extracts the organisation and sr_id information from a GeoJSON dataset.
     *
     * This method navigates through the GeoJSON dataset to find the "layers" array,
     * then the first layer's "geometryFields" array, and finally extracts the "authority"
     * and "code" from the "coordinateSystem" object of the first geometry field.
     *
     * @param geoJsonDataSetInfo the GeoJSON schema from which to extract the authority and code
     * @return a map containing the "authority" and "code" extracted from the GeoJSON schema;
     *         an empty map is returned if the "layers" or "geometryFields" arrays are empty or
     *         if the necessary fields are not found
     */

    private Map<String, String> extractCRSFromDataSetInfo(JsonObject geoJsonDataSetInfo) {
        Map<String, String> authorityAndCode = new HashMap<>();

        JsonArray layers = geoJsonDataSetInfo.getJsonArray("layers", new JsonArray());
        if (layers.isEmpty()) {
            return authorityAndCode; // Return empty map if no layers are found
        }

        JsonObject firstLayer = layers.getJsonObject(0);
        JsonArray geometryFields = firstLayer.getJsonArray("geometryFields", new JsonArray());
        if (geometryFields.isEmpty()) {
            return authorityAndCode; // Return empty map if no geometry fields are found
        }

        JsonObject geometryField = geometryFields.getJsonObject(0);
        JsonObject coordinateSystem = geometryField.getJsonObject("coordinateSystem", new JsonObject());
        JsonObject projjson = coordinateSystem.getJsonObject("projjson", new JsonObject());
        JsonObject id = projjson.getJsonObject("id", new JsonObject());

        String authority = id.getString("authority", "");
        int code = id.getInteger("code", 0);

        authorityAndCode.put("authority", authority);
        authorityAndCode.put("code", String.valueOf(code));

        return authorityAndCode;
    }


    /**
     * Retrieves the database schema (column names) for the existing collection table asynchronously.
     *
     * @param tableName The name of the database table for which the schema needs to be retrieved.
     * @return A Future resolving to a Set of column names present in the specified database table.
     *         The Future may fail if there is an error executing the database query.
     */

    private Future<Set<String>> getDbSchema(String tableName) {

        Promise<Set<String>> promise = Promise.promise();
        pgPool.withConnection(
                sqlConnection -> sqlConnection.preparedQuery(DB_SCHEMA_CHECK_QUERY)
                        .execute(Tuple.of(tableName))
                        .onSuccess(successHandler -> {
                            Set<String> columns = StreamSupport.stream(successHandler.spliterator(), false)
                                    .map(row -> row.getString("column_name"))
                                    .collect(Collectors.toSet());
                            promise.complete(columns);
                        }).onFailure(promise::fail));

        return promise.future();

    }

    /**
     * Appends data from a specified file to a temporary database table using ogr2ogr command line tool.
     * Executes the command asynchronously and completes the Future based on the execution result.
     *
     * @param requestInput The JsonObject containing input parameters for the operation, including file name and other necessary details.
     * @return A Future<Void> that completes when the data has been successfully appended to the temporary table,
     *         or fails if there is an error during the execution of ogr2ogr command.
     */

    private Future<Void> appendDataToTempTable(JsonObject requestInput) {

        String fileName = "/" + requestInput.getString("fileName");

        Promise<Void> promise = Promise.promise();

        vertx.executeBlocking(appendingPromise -> {

            CommandLine cmdLine = getOgr2ogrCommandLine(requestInput, fileName);
            LOGGER.debug("Inside Execution and the command line is: {} ",cmdLine);
            DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setExitValue(0);
            ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofHours(1)).get();
            executor.setWatchdog(watchdog);
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));

            try {
                executor.execute(cmdLine);
                LOGGER.debug("Successfully Executed");
                String outLog = stdout.toString();
                String errLog = stderr.toString();
                LOGGER.debug("ogr2ogr output: {}" , outLog);
                LOGGER.debug("ogr2ogr error output: {}" , errLog);
                appendingPromise.complete();
            } catch (IOException e) {
                String errLog = stderr.toString();
                LOGGER.error(OGR_2_OGR_FAILED_MESSAGE + errLog + e);
                appendingPromise.fail(OGR_2_OGR_FAILED_MESSAGE);
            }
        }, VERTX_EXECUTE_BLOCKING_IN_ORDER).onSuccess(handler -> {
            LOGGER.debug(APPEND_PROCESS_MESSAGE);
            promise.complete();
        }).onFailure(promise::fail);

        return promise.future();

    }

    /**
     * Constructs the command line for executing ogr2ogr to append data into a temporary PostgreSQL table.
     * Sets up various parameters such as table name, encoding, geometry name, target SRS, PostgreSQL connection details,
     * and AWS S3 configuration for input data location.
     *
     * @param requestInput The JsonObject containing input parameters for the operation, including jobId and other necessary details.
     * @param fileName The name of the file located in AWS S3 bucket to be appended into the PostgreSQL table.
     * @return A CommandLine object representing the constructed ogr2ogr command line with all necessary arguments set.
     */

    private CommandLine getOgr2ogrCommandLine(JsonObject requestInput, String fileName) {

        String jobId = requestInput.getString("jobId");
        String tempTableName = "temp_table_for_" + jobId;

        LOGGER.debug("Inside ogr2ogr command line to append data into {}",tempTableName);

        CommandLine cmdLine = new CommandLine("ogr2ogr");
        cmdLine.addArgument("-nln");
        cmdLine.addArgument(tempTableName);
        cmdLine.addArgument("-lco");
        cmdLine.addArgument("PRECISION=NO");
        cmdLine.addArgument("-lco");
        cmdLine.addArgument("LAUNDER=NO");
        cmdLine.addArgument("-lco");
        cmdLine.addArgument("GEOMETRY_NAME=geom");
        cmdLine.addArgument("-lco");
        cmdLine.addArgument("ENCODING=UTF-8");
        cmdLine.addArgument("-lco");
        cmdLine.addArgument("FID=id");

        cmdLine.addArgument("-t_srs");
        cmdLine.addArgument("EPSG:4326");
        cmdLine.addArgument("--config");
        cmdLine.addArgument("PG_USE_COPY");
        cmdLine.addArgument("NO");
        cmdLine.addArgument("-f");
        cmdLine.addArgument("PostgreSQL");
        cmdLine.addArgument(
                String.format("PG:host=%s dbname=%s user=%s port=%d password=%s schemas=public", databaseHost,
                        databaseName, databaseUser, databasePort, databasePassword), false);

        cmdLine.addArgument("-progress");
        cmdLine.addArgument("--debug");
        cmdLine.addArgument("ON");

        setS3OptionsForTesting(cmdLine);
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_S3_ENDPOINT");
        cmdLine.addArgument(awsEndPoint);
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_ACCESS_KEY_ID");
        cmdLine.addArgument(accessKey);
        cmdLine.addArgument("--config");
        cmdLine.addArgument("AWS_SECRET_ACCESS_KEY");
        cmdLine.addArgument(secretKey);

        cmdLine.addArgument(String.format("/vsis3/%s%s", awsBucketUrl, fileName));

        return cmdLine;
    }

    /**
     * Merges data from a temporary table into the existing collection table in PostgreSQL.
     * Retrieves the schema of the collection table, constructs a SQL merge query, and executes it.
     *
     * @param requestInput The JsonObject containing input parameters for the operation, including jobId, collectionsDetailsTableId,
     *                     and other necessary details.
     * @return A Future<Void> indicating the completion (or failure) of the merge operation.
     */

    private Future<Void> mergeTempTableToCollectionTable(JsonObject requestInput) {

        Promise<Void> promise = Promise.promise();
        String jobId = requestInput.getString("jobId");
        String collectionId = requestInput.getString("collectionsDetailsTableId");
        String tempTableName = "temp_table_for_" + jobId;

        getDbSchema(collectionId)
                .onSuccess(columns -> {
                    // Exclude 'id' column
                    columns.remove("id");
                    // Every column name should be put into double quotes to avoid the case sensitivity problem of Postgres
                    String columnNames = columns.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
                    String mergeQuery = String.format(
                            MERGE_TEMP_TABLE_QUERY,
                            collectionId,
                            columnNames,
                            columnNames,
                            tempTableName
                    );

                    pgPool.withConnection(sqlConnection ->
                            sqlConnection.query(mergeQuery)
                                    .execute()
                                    .onSuccess(successHandler -> {
                                        LOGGER.debug(MERGE_TEMP_TABLE_MESSAGE + " " + "for jobId - {}:",jobId);
                                        promise.complete();
                                    })
                                    .onFailure(failureHandler -> {
                                        LOGGER.error(MERGE_TEMP_TABLE_FAILURE_MESSAGE + "for jobId - {}: {}" ,jobId, failureHandler.getMessage());
                                        promise.fail(new ProcessException(500, "MERGE_FAILED", MERGE_TEMP_TABLE_FAILURE_MESSAGE));
                                    })
                    );
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Deletes the temporary table associated with the given job ID from the PostgreSQL database.
     *
     * @param requestInput The JsonObject containing input parameters for the operation, including jobId,
     *                     which is used to construct the temporary table name.
     * @return A Future<Void> indicating the completion (or failure) of the temporary table deletion operation.
     */

    private Future<Void> deleteTempTable(JsonObject requestInput) {

        Promise<Void> promise = Promise.promise();
        String jobId = requestInput.getString("jobId");
        String tempTableName = "temp_table_for_" + jobId;
        String deleteQuery = String.format(DELETE_TEMP_TABLE_QUERY, tempTableName);

        pgPool.withConnection(sqlConnection ->
                sqlConnection.query(deleteQuery)
                        .execute()
                        .onSuccess(successHandler -> {
                            LOGGER.debug(DELETE_TEMP_TABLE_SUCCESS_MESSAGE + ": "+ tempTableName);
                            promise.complete();
                        })
                        .onFailure(failureHandler -> {
                            LOGGER.fatal(DELETE_TEMP_TABLE_FAILURE_MESSAGE + ": " + tempTableName + failureHandler.getMessage());
                            promise.fail(DELETE_TEMP_TABLE_FAILURE_MESSAGE);
                        })
        );

        return promise.future();
    }

    /**
     * Handles failure scenarios by updating the job status and completing the promise with a failure.
     *
     * @param requestInput The JsonObject containing input parameters for the operation
     * @param errorMessage The error message describing the cause of the failure.
     * @param promise      The Promise to be failed with the errorMessage if the failure handling fails.
     */

    private void handleFailure(JsonObject requestInput, String errorMessage, Promise<JsonObject> promise) {

        utilClass.updateJobTableStatus(requestInput, Status.FAILED, errorMessage)
                .onSuccess(successHandler -> {
                    LOGGER.error("Process failed: {}" ,errorMessage);
                    promise.fail(errorMessage);
                })
                .onFailure(failureHandler -> {
                    LOGGER.error(HANDLE_FAILURE_MESSAGE + ": " +failureHandler.getMessage());
                    promise.fail(HANDLE_FAILURE_MESSAGE);
                });

    }

    /**
     * Calculates the progress percentage based on the current step and total steps.
     *
     * @param step       The current step in the process.
     * @param totalSteps The total number of steps in the process.
     * @return The progress percentage as a float value.
     */

    private float calculateProgress(int step, int totalSteps) {
        return (float) step / totalSteps * 100;
    }

    /**
     * Configures S3 options for integration testing. This method modifies the given
     * {@link CommandLine} object to disable SSL checks and set virtual hosting to false
     * if the "s3.mock" system property is enabled. These settings are useful for testing
     * environments where SSL certificates and S3 virtual hosting are not needed or can cause issues.
     *
     * @param ogrinfo the {@link CommandLine} object to be configured with S3 options.
     */

    private void setS3OptionsForTesting(CommandLine ogrinfo){
        if(System.getProperty("s3.mock") != null){
            LOGGER.fatal("S3 mock is enabled therefore disabling SSL check and setting Virtual hosting to false.");
            ogrinfo.addArgument("--config");
            ogrinfo.addArgument("GDAL_HTTP_UNSAFESSL");
            ogrinfo.addArgument("YES");
            ogrinfo.addArgument("--config");
            ogrinfo.addArgument("AWS_VIRTUAL_HOSTING");
            ogrinfo.addArgument("FALSE");
        }
    }


}