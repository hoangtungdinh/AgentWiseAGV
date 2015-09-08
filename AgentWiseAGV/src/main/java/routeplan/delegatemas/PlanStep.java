package routeplan.delegatemas;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Objects;

import resourceagents.FreeTimeWindow;

public class PlanStep {
  
  private final List<Point> path;
  private final int planLength;
  private final FreeTimeWindow ftw;
  
  public PlanStep(List<Point> path, int planLength, FreeTimeWindow ftw) {
    this.path = path;
    this.planLength = planLength;
    this.ftw = ftw;
  }

  public List<Point> getPath() {
    return path;
  }

  public int getPlanLength() {
    return planLength;
  }

  public FreeTimeWindow getFtw() {
    return ftw;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, planLength, ftw);
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

    return this.path == planStep.getPath()
        && this.planLength == planStep.getPlanLength()
        && this.ftw.equals(planStep.getFtw());
  }
}
