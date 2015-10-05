package routeplan.contextaware.planstepprioritygraph;

import autovalue.shaded.com.google.common.common.base.Objects;

/**
 * The Class SingleStep.
 */
public class SingleStep {

  /** The agv id. */
  private int agvID;

  /**
   * The step id to distinguish two different step of the same agv in the same
   * resource. It is the start time of the reservation of agvs on this resource
   */
  private long stepID;
  
  private boolean swappable;
  
  private boolean visited;

  public SingleStep(int agvID, long stepID) {
    this.agvID = agvID;
    this.stepID = stepID;
    this.swappable = true;
    this.visited = false;
  }

  public int getAgvID() {
    return agvID;
  }

  public long getStepID() {
    return stepID;
  }
  
  public void setNonSwappable() {
    this.swappable = false;
  }
  
  public void setVisited() {
    this.visited = true;
  }
  
  public boolean isSwappable() {
    return swappable;
  }
  
  public boolean isVisited() {
    return visited;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(agvID, stepID);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    // allows comparison with subclasses
    if (!(other instanceof SingleStep)) {
      return false;
    }

    final SingleStep singleStep = (SingleStep) other;

    return this.agvID == singleStep.getAgvID()
        && this.stepID == singleStep.getStepID();
  }

  @Override
  public String toString() {
    return ("agvID: " + agvID + "\tstepID: " + stepID);
  }
  
  
}
