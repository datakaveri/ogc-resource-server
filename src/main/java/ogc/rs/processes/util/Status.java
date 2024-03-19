package ogc.rs.processes.util;

public enum Status {
//ACCEPTED, RUNNING, SUCCESSFUL, FAILED, DISMISSED;
    ACCEPTED("Process Accepted."), RUNNING("Process Running."), SUCCESSFUL("Process Completed."), FAILED("Process Failed."),
  DISMISSED("Process Dismissed.");
  public final String message;

  Status(String message) {
    this.message = message;
  }
}
