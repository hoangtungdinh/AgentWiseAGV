package resourceagents;

import com.google.common.base.Objects;

public class PlanStep {
  private final int stage;
  private final ResourceAgent resourceAgent;
  private final FreeTimeWindow ftw;
  
  public PlanStep(int stage, ResourceAgent resourceAgent, FreeTimeWindow ftw) {
    this.stage = stage;
    this.resourceAgent = resourceAgent;
    this.ftw = ftw;
  }

  public int getStage() {
    return stage;
  }

  public ResourceAgent getResourceAgent() {
    return resourceAgent;
  }

  public FreeTimeWindow getFtw() {
    return ftw;
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
    if (!(other instanceof PlanStep)) {
      return false;
    }

    final PlanStep planStep = (PlanStep) other;

    return this.stage == planStep.getStage()
        && this.resourceAgent == planStep.resourceAgent
        && this.ftw.equals(planStep.getFtw());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(stage, resourceAgent, ftw);
  }
}
