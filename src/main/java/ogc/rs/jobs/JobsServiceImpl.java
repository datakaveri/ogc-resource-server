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
                        // Rows found, process each row
                        JsonArray jobsArray = new JsonArray();

                        rows.forEach(row -> {
                          JsonObject job = new JsonObject();
                          job.put("processID", row.getUUID("process_id").toString());
                          job.put("jobID", row.getUUID("id").toString());
                          job.put("status", row.getString("status"));
                          job.put("type", row.getString("type"));
                          job.put("message", row.getString("message"));
                          job.put("progress", row.getNumeric("progress"));

                          // Generate links for each job
                          JsonArray links = new JsonArray();
                          JsonObject link = new JsonObject();
                          link.put("href", config.getString("hostName") + "/jobs/" + row.getUUID("id").toString());
                          link.put("rel", "status");
                          link.put("title", row.getJsonObject("input").getString("title"));
                          link.put("type", "application/json");
                          link.put("hreflang", "en");
                          links.add(link);

                          job.put("links", links);
                          jobsArray.add(job);
                        });

                        JsonObject response = new JsonObject();
                        response.put("jobs", jobsArray);
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