package route;

import java.util.LinkedList;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

/**
 * The Class Route.
 * Contains the path and the schedule along the path
 *
 * @author Tung
 */
public class RoutePlan {

  /** The path. */
  private LinkedList<PlanStep> plan;
  
  /**
   * Instantiates a new route plan.
   */
  public RoutePlan() {
    plan = new LinkedList<>();
  }
  
  /**
   * Adds the next plan step.
   *
   * @param currentNode the current node
   * @param nextNode the next node
   * @param interval the interval
   */
  public void addNextStep(Point currentNode, Point nextNode,
      Range<Long> interval) {
    plan.addLast(new PlanStep(currentNode, nextNode, interval));
  }
  
  /**
   * View the next step.
   *
   * @return the next plan step
   */
  public PlanStep viewNextStep() {
    return plan.getFirst();
  }
  
  /**
   * Consume next step. Different from {@link #viewNextStep}, this method will
   * return and remove the next step from the plan
   *
   * @return the plan step
   */
  public PlanStep consumeNextStep() {
    return plan.removeFirst();
  }
}
