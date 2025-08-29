package ogc.rs.jobs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import ogc.rs.common.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JobsServiceImpl implements JobsService {
  private static final Logger LOGGER = LogManager.getLogger(JobsServiceImpl.class);
  private final PgPool pgPool;
  private final JsonObject config;

  public JobsServiceImpl(PgPool pgPool, JsonObject config) {
    this.pgPool = pgPool;
    this.config = config;
  }

  @Override
  public Future<JsonObject> getStatus(JsonObject requestBody) {
    LOGGER.info("Trying to get status");

    Promise<JsonObject> promise = Promise.promise();
    String jobId = requestBody.getString("jobId");
    String userId = requestBody.getString("userId");
    pgPool.withConnection(
      sqlClient -> sqlClient.preparedQuery("Select * from jobs_table where id=$1")
        .execute(Tuple.of(jobId)).map(s -> s.iterator().next()).onSuccess(row -> {
          if (row.getValue("user_id").toString().equals(userId)) {
            JsonObject result = row.toJson();
            result.remove("output");
            JsonObject links = new JsonObject();
            links.put("href", config.getString("hostName").concat("/jobs/").concat(jobId));
            links.put("rel", "self");
            links.put("title", row.getJsonObject("input").getString("title"));
            result.remove("input");
            result.put("links", links);
            promise.complete(result);
          } else {
            LOGGER.error("Job does not belong to the specified user");
            promise.fail(Constants.processException404);
          }
        }).onFailure(failureHandler -> {
          LOGGER.error(failureHandler.toString());
          promise.fail(Constants.processException404);
        }));

    return promise.future();
  }

  @Override
  public Future<JsonObject> listAllJobs(JsonObject requestBody) {
    LOGGER.info("Trying to list all jobs");

    Promise<JsonObject> promise = Promise.promise();
    String userId = requestBody.getString("userId");

      pgPool.withConnection(sqlClient ->
              sqlClient.preparedQuery("SELECT * FROM jobs_table WHERE user_id = $1")
                      .execute(Tuple.of(userId))
                      .onSuccess(rows -> {
                          if (!rows.iterator().hasNext()) {
                              // No rows found for the given userId
                              LOGGER.error("No jobs found for user ID: " + userId);
                              promise.fail(Constants.processException404);
                          } else {
                              JsonArray jobsArray = new JsonArray();

                              rows.forEach(row -> {
                                  JsonObject job = new JsonObject();
                                  job.put("processID", row.getUUID("process_id").toString());
                                  job.put("jobID", row.getUUID("id").toString());

                                  // Convert status to lowercase to conform to OGC enum
                                  String status = row.getString("status");
                                  job.put("status", status != null ? status.toLowerCase() : "unknown");

                                  job.put("type", row.getString("type").toLowerCase());
                                  String message = row.getString("message");
                                  job.put("message", message != null ? message : "No message");
                                  job.put("progress", row.getNumeric("progress").intValue());

                                  JsonArray links = new JsonArray();
                                  JsonObject link = new JsonObject();
                                  link.put("href", config.getString("hostName") + "/jobs/" + row.getUUID("id"));
                                  link.put("rel", "status");
                                  JsonObject input = row.getJsonObject("input");
                                  String title = (input != null && input.getString("title") != null) ? input.getString("title") : "Job details";
                                  link.put("title", title);
                                  link.put("type", "application/json");
                                  link.put("hreflang", "en");
                                  links.add(link);

                                  job.put("links", links);
                                  jobsArray.add(job);
                              });

                              // Add OGC standard top-level links
                              JsonArray topLevelLinks = new JsonArray();
                              String baseUrl = config.getString("hostName");

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/jobs")
                                      .put("rel", "self")
                                      .put("type", "application/json")
                                      .put("title", "This document"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/jobs?f=text/html")
                                      .put("rel", "alternate")
                                      .put("type", "text/html")
                                      .put("title", "This document as HTML"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/api?f=application/json")
                                      .put("rel", "service-desc")
                                      .put("type", "application/json")
                                      .put("title", "API definition for this endpoint as JSON"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/api?f=text/html")
                                      .put("rel", "service-desc")
                                      .put("type", "text/html")
                                      .put("title", "API definition for this endpoint as HTML"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/conformance")
                                      .put("rel", "http://www.opengis.net/def/rel/ogc/1.0/conformance")
                                      .put("type", "application/json")
                                      .put("title", "OGC API - Processes conformance classes implemented by this server"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/processes")
                                      .put("rel", "http://www.opengis.net/def/rel/ogc/1.0/processes")
                                      .put("type", "application/json")
                                      .put("title", "Metadata about the processes"));

                              topLevelLinks.add(new JsonObject()
                                      .put("href", baseUrl + "/jobs")
                                      .put("rel", "http://www.opengis.net/def/rel/ogc/1.0/job-list")
                                      .put("title", "The endpoint for job monitoring"));

                              JsonObject response = new JsonObject()
                                      .put("jobs", jobsArray)
                                      .put("links", topLevelLinks);

                              promise.complete(response);
                          }
                      })
                      .onFailure(failureHandler -> {
                          LOGGER.error("Failed to list jobs: " + failureHandler.toString());
                          promise.fail(Constants.processException500);
                      })
      );
    return promise.future();
  }

  @Override
  public Future<JsonObject> retrieveJobResults(JsonObject requestBody) {
    LOGGER.info("Trying to retrieve job results");

    Promise<JsonObject> promise = Promise.promise();
    String jobId = requestBody.getString("jobId");
    String userId = requestBody.getString("userId");
    pgPool.withConnection(
            sqlClient -> sqlClient.preparedQuery("Select * from jobs_table where id=$1")
                    .execute(Tuple.of(jobId)).map(s -> s.iterator().next()).onSuccess(row -> {
                      if (row.getValue("user_id").toString().equals(userId)) {
                        JsonObject rowJson = row.toJson();
                        JsonObject result = new JsonObject();
                        result.put("output", rowJson.getJsonObject("output"));
                        promise.complete(result);
                      } else {
                        LOGGER.error("Job does not belong to the specified user");
                        promise.fail(Constants.processException404);
                      }
                    }).onFailure(failureHandler -> {
                      LOGGER.error("Failed to retrieve job results: " +failureHandler.toString());
                      promise.fail(Constants.processException500);
                    }));
    return promise.future();
  }
}