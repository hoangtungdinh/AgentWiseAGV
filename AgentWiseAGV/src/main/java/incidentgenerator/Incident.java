package incidentgenerator;

public class Incident {
  
  private long startTime;
  
  private long duration;

  public Incident(long startTime, long duration) {
    this.startTime = startTime;
    this.duration = duration;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getDuration() {
    return duration;
  }

  @Override
  public String toString() {
    final String str = "Start time: " + startTime + "\t" + "Duration: "
        + duration;
    return str;
  }
  
}
