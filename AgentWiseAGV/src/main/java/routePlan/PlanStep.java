package routePlan;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

/**
 * The Class PlanStep.
 * Contains one step in the plan
 *
 * @author Tung
 */
public class PlanStep {
  
  /** 
   * The current node and the next node
   * If nextNode is different from currentNode, the AGV is traveling along an edge. 
   * If nextNode is the same as currentNode, the AGV is staying in a node.
   */
  private Point currentNode;
  private Point nextNode;
  
  /** The interval that the AGV occupying the resource. */
  private Range<Long> interval;

  /**
   * Instantiates a new plan step.
   *
   * @param currentNode the current node
   * @param nextNode the next node
   * @param interval the interval
   */
  public PlanStep(Point currentNode, Point nextNode, Range<Long> interval) {
    this.currentNode = currentNode;
    this.nextNode = nextNode;
    this.interval = interval;
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   */
  public Point getCurrentNode() {
    return currentNode;
  }

  /**
   * Gets the next node.
   *
   * @return the next node
   */
  public Point getNextNode() {
    return nextNode;
  }

  /**
   * Gets the interval.
   *
   * @return the interval
   */
  public Range<Long> getInterval() {
    return interval;
  }
  
  

}
