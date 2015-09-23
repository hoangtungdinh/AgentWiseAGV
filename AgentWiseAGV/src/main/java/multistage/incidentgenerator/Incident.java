package multistage.incidentgenerator;

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
}
