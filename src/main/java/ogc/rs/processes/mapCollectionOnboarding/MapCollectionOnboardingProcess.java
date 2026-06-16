package ogc.rs.processes.mapCollectionOnboarding;

import static ogc.rs.processes.mapCollectionOnboarding.Constants.*;
import static ogc.rs.processes.mapCollectionOnboarding.SqlConstants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import ogc.rs.common.DataFromS3;
import ogc.rs.common.S3Config;
import ogc.rs.processes.ProcessService;
import ogc.rs.processes.ProcessesRunnerImpl;
import ogc.rs.processes.featureCollectionOnboarding.FeatureCollectionOnboardingProcess;
import ogc.rs.processes.util.Status;
import ogc.rs.processes.util.UtilClass;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Onboards a pure MAP collection for OGC API Maps core. The catalogue {@code resourceId} must not
 * already exist in {@code collections_details}. The process validates the COG in S3 with {@code
 * gdalinfo} and inserts collection, enclosure, and map metadata rows.
 */
public class MapCollectionOnboardingProcess implements ProcessService {

  private static final Logger LOGGER =
      LogManager.getLogger(MapCollectionOnboardingProcess.class);
  private static final int TOTAL_STEPS = 6;
  private static final boolean VERTX_EXECUTE_BLOCKING_IN_ORDER = false;

  private final PgPool pgPool;
  private final UtilClass utilClass;
  private final FeatureCollectionOnboardingProcess collectionOnboarding;
  private final DataFromS3 dataFromS3;
  private final S3Config s3Config;
  private final Vertx vertx;

  /**
   * @param pgPool    PostgreSQL connection pool
   * @param webClient HTTP client for catalogue API calls
   * @param config    application configuration
   * @param s3conf    S3 settings for the bucket named in process input
   * @param vertx     Vert.x instance for blocking GDAL work
   */
  public MapCollectionOnboardingProcess(
      PgPool pgPool, WebClient webClient, JsonObject config, S3Config s3conf, Vertx vertx) {
    this.pgPool = pgPool;
    this.utilClass = new UtilClass(pgPool);
    this.collectionOnboarding = new FeatureCollectionOnboardingProcess(pgPool, webClient, config, s3conf, vertx);
    this.s3Config = s3conf;
    this.dataFromS3 =
        new DataFromS3(vertx.createHttpClient(new HttpClientOptions().setShared(true)), s3conf);
    this.vertx = vertx;
  }

  /**
   * Runs the map collection onboarding pipeline and updates job status/progress at each step.
   *
   * @param requestInput process inputs including {@code resourceId}, {@code fileName}, {@code title},
   *                     {@code description}, and {@code s3BucketIdentifier}
   * @return a future that completes when onboarding succeeds, or fails with an error message
   */
  @Override
  public Future<JsonObject> execute(JsonObject requestInput) {
    Promise<JsonObject> promise = Promise.promise();
    requestInput.put("progress", calculateProgress(1));

    utilClass
        .updateJobTableStatus(requestInput, Status.RUNNING, START_MAP_COLLECTION_ONBOARDING)
        .compose(v -> validateFileName(requestInput))
        .compose(v -> collectionOnboarding.makeCatApiRequest(requestInput))
        .compose(
            v ->
                utilClass.updateJobTableProgress(
                    requestInput
                        .put("progress", calculateProgress(2))
                        .put(MESSAGE, CATALOGUE_CHECK_MESSAGE)))
        .compose(v -> validateNewPureMapCollection(requestInput))
        .compose(
            v ->
                utilClass.updateJobTableProgress(
                    requestInput
                        .put("progress", calculateProgress(3))
                        .put(MESSAGE, COLLECTION_STATE_MESSAGE)))
        .compose(v -> checkFileExistenceInS3(requestInput))
        .compose(
            v ->
                utilClass.updateJobTableProgress(
                    requestInput
                        .put("progress", calculateProgress(4))
                        .put(MESSAGE, S3_FILE_CHECK_MESSAGE)))
        .compose(v -> extractRasterMetadata(requestInput))
        .compose(
            v ->
                utilClass.updateJobTableProgress(
                    requestInput
                        .put("progress", calculateProgress(5))
                        .put(MESSAGE, GDAL_METADATA_MESSAGE)))
        .compose(v -> insertMapCollectionRows(requestInput))
        .compose(
            v ->
                utilClass.updateJobTableStatus(
                    requestInput, Status.SUCCESSFUL, MAP_COLLECTION_ONBOARDING_SUCCESS_MESSAGE))
        .onSuccess(
            v -> {
              LOGGER.info(MAP_COLLECTION_ONBOARDING_SUCCESS_MESSAGE);
              promise.complete();
            })
        .onFailure(err -> handleFailure(requestInput, err.getMessage(), promise));

    return promise.future();
  }

  /** Ensures {@code fileName} ends with {@code .tif} or {@code .tiff}. */
  private Future<Void> validateFileName(JsonObject requestInput) {
    Promise<Void> promise = Promise.promise();
    if (!GdalRasterMetadataExtractor.isTiffFileName(requestInput.getString("fileName"))) {
      promise.fail(INVALID_FILE_EXTENSION_MESSAGE);
    } else {
      promise.complete();
    }
    return promise.future();
  }

  /**
   * Ensures {@code resourceId} is not already present in {@code collections_details}.
   *
   * @param requestInput process input containing {@code resourceId}
   * @return the same input on success
   */
  private Future<JsonObject> validateNewPureMapCollection(JsonObject requestInput) {
    String collectionId = requestInput.getString("resourceId");

    return pgPool
        .preparedQuery(CHECK_COLLECTION_EXISTENCE_QUERY)
        .execute(Tuple.of(collectionId))
        .compose(
            existenceRows -> {
              boolean collectionExists =
                  existenceRows.iterator().hasNext()
                      && existenceRows.iterator().next().getBoolean("exists");
              if (collectionExists) {
                return Future.failedFuture(COLLECTION_ALREADY_EXISTS_MESSAGE);
              }
              return Future.succeededFuture(requestInput);
            })
        .recover(
            err ->
                Future.failedFuture(
                    COLLECTION_EXISTENCE_CHECK_FAILURE_MESSAGE + ": " + err.getMessage()));
  }

  /**
   * Issues an S3 HEAD request for {@code fileName} and requires a non-zero content length.
   *
   * @param requestInput process input containing {@code fileName}
   * @return {@code true} when the object exists and is non-empty
   */
  private Future<Boolean> checkFileExistenceInS3(JsonObject requestInput) {
    Promise<Boolean> promise = Promise.promise();
    String fileName = requestInput.getString("fileName");
    dataFromS3.setUrlFromString(dataFromS3.getFullyQualifiedUrlString(fileName));
    dataFromS3.setSignatureHeader(HttpMethod.HEAD);

    dataFromS3
        .getDataFromS3(HttpMethod.HEAD)
        .onSuccess(
            responseFromS3 -> {
              String contentLengthHeader = responseFromS3.getHeader("Content-Length");
              BigInteger fileSize =
                  contentLengthHeader != null
                      ? new BigInteger(contentLengthHeader)
                      : BigInteger.ZERO;
              if (fileSize.compareTo(BigInteger.ZERO) > 0) {
                requestInput.put("fileSize", fileSize);
                promise.complete(true);
              } else {
                promise.fail(S3_EMPTY_FILE_MESSAGE);
              }
            })
        .onFailure(err -> promise.fail(S3_FILE_EXISTENCE_FAIL_MESSAGE));

    return promise.future();
  }

  /**
   * Runs {@code gdalinfo -json} on the COG, resolves EPSG to a CRS URI, and stores bbox/size on
   * {@code requestInput}.
   *
   * @param requestInput process input containing {@code fileName}
   * @return the input augmented with raster metadata fields
   */
  private Future<JsonObject> extractRasterMetadata(JsonObject requestInput) {
    Promise<JsonObject> promise = Promise.promise();
    vertx
        .<JsonObject>executeBlocking(
            blockingPromise -> {
              try {
                CommandLine cmd = GdalRasterMetadataExtractor.buildGdalInfoCommand(
                    s3Config, requestInput.getString("fileName"));
                DefaultExecutor executor = DefaultExecutor.builder().get();
                executor.setExitValue(0);
                ExecuteWatchdog watchdog =
                    ExecuteWatchdog.builder().setTimeout(Duration.ofMinutes(5)).get();
                executor.setWatchdog(watchdog);
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));
                int exitCode = executor.execute(cmd);
                if (exitCode != 0) {
                  String stderrText = stderr.toString(StandardCharsets.UTF_8);
                  LOGGER.error(
                      "gdalinfo failed exitCode={} stderr={}", exitCode, stderrText);
                  blockingPromise.fail(
                      GDAL_INFO_FAILED + " exitCode=" + exitCode + " stderr=" + stderrText);
                  return;
                }
                String output = stdout.toString(StandardCharsets.UTF_8).trim();
                if (output.isEmpty()) {
                  blockingPromise.fail(GDAL_INFO_FAILED + ": empty gdalinfo output");
                  return;
                }
                try {
                  JsonObject gdalJson =
                      new JsonObject(
                          Buffer.buffer(
                              output.replace("Had to open data source read-only.", "").trim()));
                  blockingPromise.complete(gdalJson);
                } catch (Exception e) {
                  blockingPromise.fail(GDAL_INFO_FAILED + ": invalid JSON output");
                }
              } catch (IOException e) {
                blockingPromise.fail(GDAL_INFO_FAILED + ": " + e.getMessage());
              }
            },
            VERTX_EXECUTE_BLOCKING_IN_ORDER)
        .compose(
            gdalJson -> {
              Integer epsgCode = GdalRasterMetadataExtractor.extractEpsgCode(gdalJson);
              if (epsgCode == null || epsgCode <= 0) {
                LOGGER.error("Could not resolve EPSG from gdalinfo JSON");
                return Future.failedFuture(CRS_ERROR);
              }
              return lookupCrsUri(epsgCode)
                  .map(
                      crsRow -> {
                        GdalRasterMetadata metadata =
                            GdalRasterMetadataExtractor.parse(
                                gdalJson, crsRow.getString("crs"), crsRow.getInteger("srid"));
                        putRasterMetadataOnInput(requestInput, metadata);
                        return requestInput;
                      });
            })
        .onSuccess(promise::complete)
        .onFailure(
            err -> {
              LOGGER.error("Raster metadata extraction failed: {}", err.getMessage());
              promise.fail(err.getMessage());
            });

    return promise.future();
  }

  /**
   * Looks up OGC CRS URI and SRID for an EPSG code in {@code crs_to_srid}.
   *
   * @param epsgCode EPSG numeric code from gdalinfo
   * @return the matching database row
   */
  private Future<Row> lookupCrsUri(int epsgCode) {
    return pgPool
        .preparedQuery(CRS_TO_SRID_SELECT_QUERY)
        .execute(Tuple.of(epsgCode))
        .compose(
            rows -> {
              if (rows.iterator().hasNext()) {
                return Future.succeededFuture(rows.iterator().next());
              }
              String crsUri = String.format(EPSG_OGC_CRS_URI_TEMPLATE, epsgCode);
              return pgPool
                  .preparedQuery(CRS_TO_SRID_INSERT_QUERY)
                  .execute(Tuple.of(crsUri, epsgCode))
                  .compose(ignored -> pgPool.preparedQuery(CRS_TO_SRID_SELECT_QUERY).execute(Tuple.of(epsgCode)))
                  .compose(
                      afterInsert -> {
                        if (!afterInsert.iterator().hasNext()) {
                          return Future.failedFuture(CRS_FETCH_FAILED + " (EPSG:" + epsgCode + ")");
                        }
                        return Future.succeededFuture(afterInsert.iterator().next());
                      });
            })
        .recover(err -> Future.failedFuture(err.getMessage()));
  }

  /**
   * Inserts all map-collection database rows in one transaction: {@code collections_details},
   * {@code ri_details}, {@code collection_supported_crs}, {@code collection_type} (MAP),
   * {@code collections_enclosure}, and {@code collection_map_metadata}.
   *
   * @param requestInput process input with catalogue, raster, and access fields populated
   * @return a future that completes when all inserts succeed
   */
  private Future<Void> insertMapCollectionRows(JsonObject requestInput) {
    String collectionId = requestInput.getString("resourceId");
    String title = requestInput.getString("title");
    String description = requestInput.getString("description");
    String crsUri = requestInput.getString("nativeCrsUri");
    Double[] wgs84Bbox = toDoubleArray(requestInput.getJsonArray("wgs84Bbox"));
    Double[] contentBbox = toDoubleArray(requestInput.getJsonArray("contentBbox"));
    int nativeSrid = requestInput.getInteger("nativeSrid");
    Integer rasterWidth = requestInput.getInteger("rasterWidth");
    Integer rasterHeight = requestInput.getInteger("rasterHeight");
    String[] temporalArray = temporalArray(requestInput.getJsonArray("temporal"));
    String accessPolicy = requestInput.getString("accessPolicy");
    String userId = requestInput.getString("userId");
    String s3BucketId =
        requestInput.getString(ProcessesRunnerImpl.S3_BUCKET_IDENTIFIER_PROCESS_INPUT_KEY);
    String href = requestInput.getString("fileName");
    String wmsUrl = requestInput.getString("wmsUrl");
    BigInteger fileSize = (BigInteger) requestInput.getValue("fileSize");

    return pgPool.withTransaction(
        sqlClient -> {
          Promise<Void> promise = Promise.promise();

          sqlClient
              .preparedQuery(INSERT_COLLECTION_DETAILS_QUERY)
              .execute(
                  Tuple.of(
                      collectionId,
                      title,
                      description,
                      crsUri,
                      wgs84Bbox,
                      temporalArray))
              .mapEmpty()
              .compose(
                  v ->
                      sqlClient
                          .preparedQuery(INSERT_RI_DETAILS_QUERY)
                          .execute(Tuple.of(collectionId, accessPolicy, userId))
                          .mapEmpty())
              .compose(
                  v ->
                      sqlClient
                          .preparedQuery(COLLECTION_SUPPORTED_CRS_INSERT_QUERY)
                          .execute(Tuple.of(collectionId, nativeSrid))
                          .mapEmpty())
              .compose(
                  v ->
                      sqlClient
                          .preparedQuery(INSERT_COLLECTION_TYPE_QUERY)
                          .execute(Tuple.of(collectionId, MAP_COLLECTION_TYPE))
                          .mapEmpty())
              .compose(
                  v ->
                      sqlClient
                          .preparedQuery(INSERT_COLLECTIONS_ENCLOSURE_QUERY)
                          .execute(
                              Tuple.of(
                                  collectionId,
                                  title,
                                  href,
                                  MAP_ENCLOSURE_MIME_TYPE,
                                  fileSize,
                                  s3BucketId))
                          .mapEmpty())
              .compose(
                  v ->
                      sqlClient
                          .preparedQuery(INSERT_COLLECTION_MAP_METADATA_QUERY)
                          .execute(
                              Tuple.of(
                                  collectionId,
                                  href,
                                  s3BucketId,
                                  contentBbox,
                                  wmsUrl,
                                  rasterWidth,
                                  rasterHeight))
                          .mapEmpty())
              .onSuccess(v -> promise.complete())
              .onFailure(err -> promise.fail(ONBOARDING_FAILED_DB_ERROR + ": " + err.getMessage()));

          return promise.future();
        });
  }

  /** Converts an optional temporal {@link JsonArray} to a PostgreSQL text array. */
  private static String[] temporalArray(JsonArray temporal) {
    if (temporal == null || temporal.isEmpty()) {
      return new String[0];
    }
    return temporal.stream().map(Object::toString).toArray(String[]::new);
  }

  /** Copies parsed GDAL metadata onto {@code requestInput} for {@link #insertMapCollectionRows}. */
  private static void putRasterMetadataOnInput(JsonObject requestInput, GdalRasterMetadata metadata) {
    requestInput.put("nativeCrsUri", metadata.getCrsUri());
    requestInput.put("nativeSrid", metadata.getSrid());
    requestInput.put("contentBbox", toJsonArray(metadata.getContentBbox()));
    requestInput.put("wgs84Bbox", toJsonArray(metadata.getWgs84Bbox()));
    if (metadata.getRasterWidth() != null) {
      requestInput.put("rasterWidth", metadata.getRasterWidth());
    }
    if (metadata.getRasterHeight() != null) {
      requestInput.put("rasterHeight", metadata.getRasterHeight());
    }
  }

  /** Converts a four-element bbox array to a {@link JsonArray}. */
  private static JsonArray toJsonArray(double[] values) {
    JsonArray array = new JsonArray();
    for (double value : values) {
      array.add(value);
    }
    return array;
  }

  /** Converts a four-element {@link JsonArray} bbox to {@code Double[]} for SQL binding. */
  private static Double[] toDoubleArray(JsonArray array) {
    if (array == null || array.size() < 4) {
      throw new IllegalArgumentException("Invalid bbox array");
    }
    return array.stream()
        .map(obj -> ((Number) obj).doubleValue())
        .toArray(Double[]::new);
  }

  /** Marks the job as failed and completes the outer promise with {@code errorMessage}. */
  private void handleFailure(
      JsonObject requestInput, String errorMessage, Promise<JsonObject> promise) {
    String message = errorMessage;
    LOGGER.error("Map collection onboarding failed: {}", message);
    utilClass
        .updateJobTableStatus(requestInput, Status.FAILED, message)
        .onSuccess(v -> promise.fail(message))
        .onFailure(err -> promise.fail(HANDLE_FAILURE_MESSAGE));
  }

  /** Returns job progress as a percentage for step {@code currentStep} of {@link #TOTAL_STEPS}. */
  private float calculateProgress(int currentStep) {
    return ((float) currentStep / TOTAL_STEPS) * 100;
  }
}
