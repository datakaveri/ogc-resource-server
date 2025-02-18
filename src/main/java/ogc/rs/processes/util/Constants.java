package ogc.rs.processes.util;

public class Constants {
  public static final String UPDATE_JOB_TABLE_STATUS_QUERY =
      "UPDATE JOBS_TABLE SET UPDATED_AT = NOW(), STARTED_AT = CASE WHEN $1 = 'RUNNING' THEN NOW() ELSE STARTED_AT END, FINISHED_AT = CASE WHEN $1 IN ('FAILED', 'SUCCESSFUL') THEN NOW() ELSE NULL END, PROGRESS = CASE WHEN $1 = 'SUCCESSFUL' THEN 100.0 WHEN $1 = 'RUNNING' THEN 14.28  ELSE PROGRESS END, STATUS = $1::JOB_STATUS_TYPE, MESSAGE = $2 WHERE ID = $3;";
  public static final String UPDATE_JOB_STATUS_PROGRESS =
      "UPDATE JOBS_TABLE SET PROGRESS = $1,MESSAGE= $2 WHERE ID = $3;";
  public static final String UPDATE_JOB_TABLE_OUTPUT =
          "UPDATE JOBS_TABLE SET OUTPUT = $1 WHERE ID = $2;";
  public static final String NEW_JOB_INSERT_QUERY =
      "INSERT INTO JOBS_TABLE (process_id,user_id,created_at,"
          + "updated_at,input,output,progress,status,type,message) VALUES($1,$2,NOW(),NOW(),"
          + "$3,$4,'0.0',$5,'PROCESS',$6) RETURNING ID,status;";
  public static final String PROCESS_EXIST_CHECK_QUERY =
      "SELECT * FROM PROCESSES_TABLE WHERE ID=$1";
  public static final String PROCESS_ACCEPTED_RESPONSE =
      "Process accepted. Starting the execution..";
}
