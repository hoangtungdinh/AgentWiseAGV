package routeplan.contextaware.planstepprioritygraph;

import autovalue.shaded.com.google.common.common.base.Objects;
import resourceagents.ResourceAgent;

/**
 * The Class SingleStep.
 */
public class SingleStep {

  /** The resource agent corresponding to this step. */
  private ResourceAgent resourceAgent;

  /** The agv id. */
  private int agvID;

  /**
   * The step id to distinguish two different step of the same agv in the same
   * resource. It is the start time of the reservation of agvs on this resource
   */
  private long stepID;

  public SingleStep(ResourceAgent resourceAgent, int agvID, long stepID) {
    this.resourceAgent = resourceAgent;
    this.agvID = agvID;
    this.stepID = stepID;
  }

  public ResourceAgent getResourceAgent() {
    return resourceAgent;
  }

  public int getAgvID() {
    return agvID;
  }

  public long getStepID() {
    return stepID;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(resourceAgent, agvID, stepID);
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

    return this.resourceAgent == singleStep.getResourceAgent()
        && this.agvID == singleStep.getAgvID()
        && this.stepID == singleStep.getStepID();
  }

  @Override
  public String toString() {
    return ("agvID: " + agvID + "\tstepID: " + stepID + "\tResource: "
        + resourceAgent);
  }
  
  
}
